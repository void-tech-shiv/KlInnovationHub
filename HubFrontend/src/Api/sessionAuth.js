/**
 * Role-aware client session handling.
 *
 * Tokens live under their existing storage keys for backward compatibility.
 * Never clear the entire origin's localStorage when one role expires.
 */
export const SESSION_KEYS = Object.freeze({
  student: ["token", "student", "studentId"],
  reviewer: ["reviewerToken", "reviewer", "reviewerId"],
  admin: ["adminToken", "admin"],
});

const TOKEN_KEYS = Object.freeze({
  student: "token",
  reviewer: "reviewerToken",
  admin: "adminToken",
});

const PROFILE_ROLE_KEY = "activeProfileRole";

export function getRoleToken(role, storage = localStorage) {
  const tokenKey = TOKEN_KEYS[role];
  return tokenKey ? storage.getItem(tokenKey) : null;
}

export function setActiveProfileRole(role, storage = localStorage) {
  if (role !== "student" && role !== "reviewer") {
    throw new Error("Invalid profile role");
  }
  storage.setItem(PROFILE_ROLE_KEY, role);
}

export function getSharedAuthRole(storage = localStorage) {
  const active = storage.getItem(PROFILE_ROLE_KEY);
  if ((active === "student" || active === "reviewer")
      && getRoleToken(active, storage)) {
    return active;
  }

  const student = getRoleToken("student", storage);
  const reviewer = getRoleToken("reviewer", storage);
  if (student && !reviewer) return "student";
  if (reviewer && !student) return "reviewer";
  // Legacy sessions without an active-role marker are deterministic. Never
  // overwrite a caller-specified role based on token ordering.
  return student ? "student" : reviewer ? "reviewer" : null;
}

export function clearRoleSession(role, storage = localStorage) {
  const keys = SESSION_KEYS[role];
  if (!keys) return;
  keys.forEach((key) => storage.removeItem(key));
  if (storage.getItem(PROFILE_ROLE_KEY) === role) {
    storage.removeItem(PROFILE_ROLE_KEY);
  }
}

/**
 * Attach exactly one requested role's token. Login requests must use
 * skipAuth:true so an unrelated/stale token cannot invalidate the session.
 */
export function attachRoleToken(config, role, storage = localStorage) {
  config.headers = config.headers || {};
  config._sessionRole = null;
  config._sessionToken = null;

  if (config.skipAuth || !TOKEN_KEYS[role]) {
    delete config.headers.Authorization;
    return config;
  }
  const token = getRoleToken(role, storage);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    config._sessionRole = role;
    config._sessionToken = token;
  } else {
    delete config.headers.Authorization;
  }
  return config;
}

/**
 * Remove only the role used by the failed request, and only if it still
 * matches the saved token. An old in-flight 401 cannot delete a new login.
 */
export function clearExpiredRequestSession(config, storage = localStorage) {
  if (!config || config.skipAuth || !config._sessionRole || !config._sessionToken) {
    return null;
  }
  const role = config._sessionRole;
  if (getRoleToken(role, storage) !== config._sessionToken) {
    return null;
  }
  clearRoleSession(role, storage);
  return role;
}
