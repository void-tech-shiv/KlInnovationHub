import { readFileSync } from "node:fs";

const env = readFileSync(new URL("../.env.production", import.meta.url), "utf8");
const line = env.split(/\r?\n/).find((entry) => /^\s*VITE_API_URL\s*=/.test(entry));
const rawValue = line?.split("=").slice(1).join("=").trim().replace(/^['"]|['"]$/g, "");

if (!rawValue) {
  console.error("RELEASE BLOCKED: VITE_API_URL is not configured in .env.production.");
  process.exit(1);
}

let url;
try {
  url = new URL(rawValue);
} catch {
  console.error("RELEASE BLOCKED: VITE_API_URL is not a valid URL.");
  process.exit(1);
}

if (url.protocol !== "https:" || !url.hostname || url.username || url.password) {
  console.error(
    "RELEASE BLOCKED: production VITE_API_URL must use HTTPS with no embedded credentials."
  );
  process.exit(1);
}

console.log("Production API transport configuration uses HTTPS.");
