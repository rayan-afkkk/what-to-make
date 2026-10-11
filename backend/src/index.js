// What to Cook API — Cloudflare Worker + Turso.
//
// POST   /auth/guest        -> { userId, token }                (no auth)
// GET    /usage             -> { plan, used, limit, resetsAt }
// POST   /recipe            -> { recipes: [...], usage: {...} } | 429 { error, resetsAt }
// POST   /verify-purchase   -> { plan, expiresAt }
// DELETE /account           -> 204
//
// Every route except /auth/guest needs "Authorization: Bearer <token>".
// Tokens are random; only their SHA-256 hash is stored in Turso.

import { createClient } from "@libsql/client/web";
import { generateRecipes, RecipeGenerationError } from "./gemini.js";
import { verifySubscription, PlayVerificationError } from "./play.js";
import { base64Url } from "./util.js";

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;
const GUEST_SIGNUPS_PER_HOUR = 10;

export default {
  async fetch(request, env, ctx) {
    try {
      return await route(request, env, ctx);
    } catch (error) {
      console.error("Unhandled error", error);
      return json({ error: "server_error" }, 500);
    }
  },
};

async function route(request, env, ctx) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";
  const method = request.method;
  const db = database(env);

  if (method === "GET" && path === "/") return json({ ok: true, service: "what-to-cook-api" });
  if (method === "POST" && path === "/auth/guest") return createGuest(request, db, ctx);

  const userId = await authenticate(request, db);
  if (!userId) return json({ error: "unauthorized" }, 401);

  if (method === "GET" && path === "/usage") return getUsage(db, env, userId);
  if (method === "POST" && path === "/recipe") return createRecipes(request, db, env, userId);
  if (method === "POST" && path === "/verify-purchase") return verifyPurchase(request, db, env, userId);
  if (method === "DELETE" && path === "/account") return deleteAccount(db, userId);

  return json({ error: "not_found" }, 404);
}

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------

async function createGuest(request, db, ctx) {
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (!(await allowRequest(db, `guest:${await sha256(ip)}`, GUEST_SIGNUPS_PER_HOUR, 60 * 60 * 1000, ctx))) {
    return json({ error: "too_many_requests" }, 429);
  }
  const userId = crypto.randomUUID();
  const token = randomToken();
  const now = Date.now();
  await db.batch(
    [
      { sql: "INSERT INTO users (id, plan, created_at) VALUES (?, 'free', ?)", args: [userId, now] },
      { sql: "INSERT INTO sessions (token_hash, user_id, created_at) VALUES (?, ?, ?)", args: [await sha256(token), userId, now] },
    ],
    "write",
  );
  return json({ userId, token });
}

async function authenticate(request, db) {
  const header = request.headers.get("Authorization") || "";
  if (!header.startsWith("Bearer ")) return null;
  const token = header.slice(7).trim();
  if (token.length < 20 || token.length > 200) return null;
  const result = await db.execute({
    sql: "SELECT user_id FROM sessions WHERE token_hash = ?",
    args: [await sha256(token)],
  });
  return result.rows[0]?.user_id ?? null;
}

// ---------------------------------------------------------------------------
// Usage
// ---------------------------------------------------------------------------

/** Weeks start Monday 00:00 UTC. */
function currentWeekStart(now = Date.now()) {
  const d = new Date(now);
  const daysSinceMonday = (d.getUTCDay() + 6) % 7;
  return Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() - daysSinceMonday);
}

async function planFor(db, env, userId) {
  const result = await db.execute({ sql: "SELECT plan, pro_expires_at FROM users WHERE id = ?", args: [userId] });
  const row = result.rows[0];
  const isPro = row && row.plan === "pro" && Number(row.pro_expires_at || 0) > Date.now();
  return {
    plan: isPro ? "pro" : "free",
    limit: isPro ? intVar(env.PRO_WEEKLY_LIMIT, 100) : intVar(env.FREE_WEEKLY_LIMIT, 7),
  };
}

async function getUsage(db, env, userId) {
  const weekStart = currentWeekStart();
  const { plan, limit } = await planFor(db, env, userId);
  const result = await db.execute({
    sql: "SELECT used FROM usage WHERE user_id = ? AND week_start = ?",
    args: [userId, weekStart],
  });
  const used = Number(result.rows[0]?.used ?? 0);
  return json({ plan, used, limit, resetsAt: weekStart + WEEK_MS });
}

/** Atomically takes one recipe from this week's allowance. Returns the new count or null if none left. */
async function reserveSlot(db, userId, weekStart, limit) {
  const results = await db.batch(
    [
      { sql: "INSERT OR IGNORE INTO usage (user_id, week_start, used) VALUES (?, ?, 0)", args: [userId, weekStart] },
      {
        sql: "UPDATE usage SET used = used + 1 WHERE user_id = ? AND week_start = ? AND used < ? RETURNING used",
        args: [userId, weekStart, limit],
      },
    ],
    "write",
  );
  const row = results[1].rows[0];
  return row ? Number(row.used) : null;
}

async function releaseSlot(db, userId, weekStart) {
  await db.execute({
    sql: "UPDATE usage SET used = used - 1 WHERE user_id = ? AND week_start = ? AND used > 0",
    args: [userId, weekStart],
  });
}

// ---------------------------------------------------------------------------
// Recipes
// ---------------------------------------------------------------------------

async function createRecipes(request, db, env, userId) {
  const body = await readJson(request);
  const input = validateRecipeRequest(body);
  if (!input) return json({ error: "bad_request" }, 400);

  const weekStart = currentWeekStart();
  const resetsAt = weekStart + WEEK_MS;
  const { limit } = await planFor(db, env, userId);
  const used = await reserveSlot(db, userId, weekStart, limit);
  if (used === null) return json({ error: "limit_reached", resetsAt }, 429);

  try {
    const recipes = await generateRecipes(env, input);
    return json({ recipes, usage: { used, limit, resetsAt } });
  } catch (error) {
    // The user didn't get recipes, so they shouldn't lose a free one.
    await releaseSlot(db, userId, weekStart);
    console.error("Recipe generation failed", error);
    const timeout = error instanceof RecipeGenerationError && error.timeout;
    return json({ error: timeout ? "timeout" : "generation_failed" }, timeout ? 504 : 502);
  }
}

function validateRecipeRequest(body) {
  if (!body || !Array.isArray(body.ingredients)) return null;
  const cleanText = (value, max) =>
    typeof value === "string" && value.trim() ? value.trim().slice(0, max) : null;
  const ingredients = body.ingredients
    .map((item) => cleanText(item, 60))
    .filter(Boolean)
    .slice(0, 80);
  const equipment = Array.isArray(body.equipment)
    ? body.equipment.map((item) => cleanText(item, 40)).filter(Boolean).slice(0, 10)
    : null;
  const timeMinutes = Number.isInteger(body.timeMinutes) && body.timeMinutes > 0 ? Math.min(body.timeMinutes, 240) : null;
  const allowMissing = Number.isInteger(body.allowMissing) ? Math.max(0, Math.min(body.allowMissing, 10)) : 2;
  return {
    ingredients,
    diet: cleanText(body.diet, 60),
    timeMinutes,
    equipment: equipment && equipment.length ? equipment : null,
    cuisine: cleanText(body.cuisine, 60),
    allowMissing,
  };
}

// ---------------------------------------------------------------------------
// Purchases
// ---------------------------------------------------------------------------

async function verifyPurchase(request, db, env, userId) {
  const body = await readJson(request);
  const purchaseToken = typeof body?.purchaseToken === "string" ? body.purchaseToken.trim() : "";
  const productId = typeof body?.productId === "string" ? body.productId.trim() : "";
  if (!purchaseToken || purchaseToken.length > 1000 || !productId) return json({ error: "bad_request" }, 400);
  if (!env.GOOGLE_SERVICE_ACCOUNT_JSON) return json({ error: "billing_not_configured" }, 503);

  let subscription;
  try {
    subscription = await verifySubscription(env, purchaseToken, productId);
  } catch (error) {
    console.error("Play verification failed", error);
    const status = error instanceof PlayVerificationError ? error.status : 502;
    return json({ error: "verification_failed" }, status);
  }
  if (!subscription.active) return json({ plan: "free", expiresAt: subscription.expiresAt ?? 0 });

  const now = Date.now();
  // A purchase belongs to the Google account, so a reinstall (new guest) can claim it again.
  await db.batch(
    [
      {
        sql: `INSERT INTO purchases (purchase_token, user_id, product_id, expires_at, created_at)
              VALUES (?, ?, ?, ?, ?)
              ON CONFLICT(purchase_token) DO UPDATE SET user_id = excluded.user_id, expires_at = excluded.expires_at`,
        args: [purchaseToken, userId, productId, subscription.expiresAt, now],
      },
      { sql: "UPDATE users SET plan = 'pro', pro_expires_at = ? WHERE id = ?", args: [subscription.expiresAt, userId] },
    ],
    "write",
  );
  return json({ plan: "pro", expiresAt: subscription.expiresAt });
}

// ---------------------------------------------------------------------------
// Account deletion (required by Google Play)
// ---------------------------------------------------------------------------

async function deleteAccount(db, userId) {
  await db.batch(
    [
      { sql: "DELETE FROM sessions WHERE user_id = ?", args: [userId] },
      { sql: "DELETE FROM usage WHERE user_id = ?", args: [userId] },
      { sql: "DELETE FROM purchases WHERE user_id = ?", args: [userId] },
      { sql: "DELETE FROM users WHERE id = ?", args: [userId] },
    ],
    "write",
  );
  return new Response(null, { status: 204 });
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function database(env) {
  if (!env.TURSO_DATABASE_URL || !env.TURSO_AUTH_TOKEN) {
    throw new Error("TURSO_DATABASE_URL / TURSO_AUTH_TOKEN secrets are missing. Run `npm run setup`.");
  }
  return createClient({ url: env.TURSO_DATABASE_URL, authToken: env.TURSO_AUTH_TOKEN });
}

/** Fixed-window rate limit stored in Turso. */
async function allowRequest(db, key, max, windowMs, ctx) {
  const now = Date.now();
  const result = await db.execute({
    sql: `INSERT INTO rate_limits (key, count, expires_at) VALUES (?, 1, ?)
          ON CONFLICT(key) DO UPDATE SET
            count = CASE WHEN rate_limits.expires_at < ? THEN 1 ELSE rate_limits.count + 1 END,
            expires_at = CASE WHEN rate_limits.expires_at < ? THEN excluded.expires_at ELSE rate_limits.expires_at END
          RETURNING count`,
    args: [key, now + windowMs, now, now],
  });
  if (Math.random() < 0.02) {
    ctx.waitUntil(db.execute({ sql: "DELETE FROM rate_limits WHERE expires_at < ?", args: [now] }).catch(() => {}));
  }
  return Number(result.rows[0]?.count ?? 0) <= max;
}

function randomToken() {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    return null;
  }
}

function intVar(value, fallback) {
  const n = parseInt(value, 10);
  return Number.isFinite(n) && n >= 0 ? n : fallback;
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}
