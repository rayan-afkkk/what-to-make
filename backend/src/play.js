// Google Play subscription verification (Play Developer API, subscriptionsv2).
// Needs the GOOGLE_SERVICE_ACCOUNT_JSON secret: a service account with access to
// your app in Play Console (Users and permissions -> invite the service account email).

import { base64Url } from "./util.js";

const ACTIVE_STATES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  // Auto-renew turned off but still paid until expiry.
  "SUBSCRIPTION_STATE_CANCELED",
]);

export class PlayVerificationError extends Error {
  constructor(message, status = 502) {
    super(message);
    this.status = status;
  }
}

let cachedToken = null; // { value, expiresAt }

export async function verifySubscription(env, purchaseToken, productId) {
  const accessToken = await googleAccessToken(env);
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/` +
    `${encodeURIComponent(env.PACKAGE_NAME)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  const response = await fetch(url, { headers: { authorization: `Bearer ${accessToken}` } });
  if (response.status === 400 || response.status === 404 || response.status === 410) {
    throw new PlayVerificationError(`Play rejected the token (${response.status})`, 400);
  }
  if (!response.ok) throw new PlayVerificationError(`Play API HTTP ${response.status}: ${await response.text()}`);

  const sub = await response.json();
  const expectedProduct = env.PRO_PRODUCT_ID || productId;
  const expiresAt = (sub.lineItems || [])
    .filter((item) => item.productId === expectedProduct)
    .map((item) => Date.parse(item.expiryTime))
    .filter(Number.isFinite)
    .reduce((max, t) => Math.max(max, t), 0);

  return {
    active: ACTIVE_STATES.has(sub.subscriptionState) && expiresAt > Date.now(),
    expiresAt,
  };
}

async function googleAccessToken(env) {
  if (cachedToken && cachedToken.expiresAt > Date.now() + 60_000) return cachedToken.value;

  let account;
  try {
    account = JSON.parse(env.GOOGLE_SERVICE_ACCOUNT_JSON);
  } catch {
    throw new PlayVerificationError("GOOGLE_SERVICE_ACCOUNT_JSON is not valid JSON", 503);
  }
  const now = Math.floor(Date.now() / 1000);
  const encoder = new TextEncoder();
  const header = base64Url(encoder.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64Url(
    encoder.encode(
      JSON.stringify({
        iss: account.client_email,
        scope: "https://www.googleapis.com/auth/androidpublisher",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      }),
    ),
  );
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, encoder.encode(`${header}.${claims}`));
  const assertion = `${header}.${claims}.${base64Url(new Uint8Array(signature))}`;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!response.ok) throw new PlayVerificationError(`Google OAuth HTTP ${response.status}: ${await response.text()}`);
  const data = await response.json();
  cachedToken = { value: data.access_token, expiresAt: Date.now() + (data.expires_in || 3600) * 1000 };
  return cachedToken.value;
}

function pemToDer(pem) {
  const base64 = String(pem || "")
    .replace(/-----BEGIN [^-]+-----/, "")
    .replace(/-----END [^-]+-----/, "")
    .replace(/\s+/g, "");
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}
