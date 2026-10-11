// One-command setup: installs packages, creates the Turso tables, logs in to
// Cloudflare, deploys the Worker and stores your secrets. Run from this folder:
//
//   npm run setup
//
// Secrets are typed into THIS terminal only and sent straight to Cloudflare as
// encrypted Worker secrets. They are never written to a file.

import { spawnSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import readline from "node:readline";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
process.chdir(root);
const DEFAULT_DB_URL = "libsql://what-to-cook-what-to-cook.aws-ap-south-1.turso.io";
const env = { ...process.env, WRANGLER_SEND_METRICS: "false" };

const say = (text = "") => console.log(text);
const step = (n, text) => say(`\n\x1b[1m[${n}/6] ${text}\x1b[0m`);
const fail = (text) => {
  say(`\n\x1b[31m✖ ${text}\x1b[0m`);
  process.exit(1);
};

function run(command, args, options = {}) {
  return spawnSync(command, args, { stdio: "inherit", shell: true, env, ...options });
}

function ask(question, { hidden = false, fallback = "" } = {}) {
  return new Promise((resolve) => {
    const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: true });
    process.stdout.write(question);
    if (hidden) {
      // Show one * per keystroke/paste instead of the secret itself.
      rl._writeToOutput = (text) => {
        if (text && !/[\r\n]/.test(text)) rl.output.write("*");
      };
    }
    rl.question("", (answer) => {
      rl.close();
      if (hidden) process.stdout.write("\n");
      resolve(answer.trim() || fallback);
    });
  });
}

// ---------------------------------------------------------------------------

say("\n🍳 What to Cook backend setup\n");

step(1, "Installing packages (first time takes a minute)");
if (!existsSync(join(root, "node_modules", "@libsql", "client")) || !existsSync(join(root, "node_modules", "wrangler"))) {
  if (run("npm", ["install", "--no-fund", "--no-audit"]).status !== 0) fail("npm install failed. Check your internet connection and try again.");
} else {
  say("Already installed.");
}
const { SCHEMA } = await import(pathToFileURL(join(root, "src", "schema.js")).href);
const { createClient } = await import("@libsql/client");

step(2, "Turso database");
let dbUrl;
let dbToken;
while (true) {
  dbUrl = await ask(`Turso database URL [press Enter for ${DEFAULT_DB_URL}]: `, { fallback: DEFAULT_DB_URL });
  dbToken = await ask("Turso token (use a NEW token, the input is hidden): ", { hidden: true });
  if (!dbToken) {
    say("A token is required. Create one in the Turso dashboard: your database > Create Token > Read & Write.");
    continue;
  }
  try {
    const db = createClient({ url: dbUrl, authToken: dbToken });
    for (const statement of SCHEMA) await db.execute(statement);
    db.close();
    say("✔ Connected. Tables are ready.");
    break;
  } catch (error) {
    say(`Couldn't connect to Turso (${error.message || error}). Check the URL and token and try again.\n`);
  }
}

step(3, "Gemini API key (makes the recipes)");
say("Get a free key at https://aistudio.google.com/apikey , click 'Create API key', then paste it here.");
let geminiKey = "";
while (!geminiKey) geminiKey = await ask("Gemini API key (hidden): ", { hidden: true });

say("\nOptional: Google Play service account JSON file, for Pro subscriptions.");
say("Skip this until your app is on Play Console (run `npm run setup` again later).");
let playJson = "";
while (true) {
  const path = (await ask("Path to the .json file [press Enter to skip]: ")).replace(/^"|"$/g, "");
  if (!path) break;
  try {
    const parsed = JSON.parse(readFileSync(path, "utf8"));
    if (!parsed.client_email || !parsed.private_key) throw new Error("not a service account key");
    playJson = JSON.stringify(parsed);
    say("✔ Service account loaded.");
    break;
  } catch (error) {
    say(`Couldn't read that file (${error.message}). Try again or press Enter to skip.`);
  }
}

step(4, "Cloudflare login");
const whoami = run("npx", ["wrangler", "whoami"], { stdio: ["inherit", "pipe", "pipe"], encoding: "utf8" });
if (/not authenticated|not logged in/i.test(`${whoami.stdout}${whoami.stderr}`) || whoami.status !== 0) {
  say("A browser window will open. Sign up or log in to Cloudflare (free), then click Allow.");
  if (run("npx", ["wrangler", "login"]).status !== 0) fail("Cloudflare login failed. Run `npm run setup` again.");
} else {
  say("Already logged in.");
}

step(5, "Deploying the backend");
let url = "";
const deploy = run("npx", ["wrangler", "deploy"], { stdio: ["inherit", "pipe", "inherit"], encoding: "utf8" });
process.stdout.write(deploy.stdout || "");
url = (deploy.stdout || "").match(/https:\/\/[^\s]+\.workers\.dev/)?.[0] || "";
if (deploy.status !== 0) {
  say("\nRetrying interactively (Cloudflare may ask you to pick a free workers.dev name)...");
  if (run("npx", ["wrangler", "deploy"]).status !== 0) fail("Deploy failed. Read the message above, then run `npm run setup` again.");
}
while (!/^https:\/\/\S+$/.test(url)) {
  url = await ask("Paste the https://....workers.dev link shown above: ");
}

step(6, "Saving secrets to Cloudflare (encrypted)");
const secrets = { TURSO_DATABASE_URL: dbUrl, TURSO_AUTH_TOKEN: dbToken, GEMINI_API_KEY: geminiKey };
if (playJson) secrets.GOOGLE_SERVICE_ACCOUNT_JSON = playJson;
for (const [name, value] of Object.entries(secrets)) {
  const result = run("npx", ["wrangler", "secret", "put", name], { input: value, stdio: ["pipe", "inherit", "inherit"] });
  if (result.status !== 0) fail(`Couldn't save ${name}. Run \`npm run setup\` again.`);
}

say("\nChecking the backend...");
try {
  const response = await fetch(url);
  say(response.ok ? "✔ Backend is answering." : `Backend answered with HTTP ${response.status}; it may need a few seconds.`);
} catch {
  say("Couldn't reach it yet; new workers.dev links can take a minute to go live.");
}

say(`\n\x1b[32m✅ Done! Your backend link:\n\n   ${url}/\n\x1b[0m`);
say("Send that link to Claude (it is not secret) and the app will be switched to use it.");
say("Never share the Turso token or Gemini key with anyone.\n");
