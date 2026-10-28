import test from "node:test";
import assert from "node:assert/strict";

import {
  attachRoleToken,
  clearExpiredRequestSession,
  clearRoleSession,
  getRoleToken,
  getSharedAuthRole,
  setActiveProfileRole,
} from "./sessionAuth.js";

class FakeStorage {
  values = new Map();
  getItem(key) { return this.values.has(key) ? this.values.get(key) : null; }
  setItem(key, value) { this.values.set(key, String(value)); }
  removeItem(key) { this.values.delete(key); }
}

function sessions() {
  const s = new FakeStorage();
  s.setItem("token", "student-secret");
  s.setItem("student", '{"id":1}');
  s.setItem("studentId", "1");
  s.setItem("reviewerToken", "reviewer-secret");
  s.setItem("reviewer", '{"id":2}');
  s.setItem("reviewerId", "2");
  s.setItem("adminToken", "admin-secret");
  s.setItem("admin", '{"id":3}');
  s.setItem("theme", "dark");
  return s;
}

test("student expiry clears only student session, never reviewer/admin or preferences", () => {
  const storage = sessions();
  setActiveProfileRole("student", storage);
  const config = attachRoleToken({ headers: {} }, "student", storage);

  assert.equal(config.headers.Authorization, "Bearer student-secret");
  assert.equal(clearExpiredRequestSession(config, storage), "student");
  assert.equal(getRoleToken("student", storage), null);
  assert.equal(storage.getItem("student"), null);
  assert.equal(storage.getItem("studentId"), null);
  assert.equal(getRoleToken("reviewer", storage), "reviewer-secret");
  assert.equal(getRoleToken("admin", storage), "admin-secret");
  assert.equal(storage.getItem("theme"), "dark");
  assert.equal(storage.getItem("activeProfileRole"), null);
});

test("reviewer expiry does not log out student or admin", () => {
  const storage = sessions();
  setActiveProfileRole("reviewer", storage);
  const config = attachRoleToken({ headers: {} }, "reviewer", storage);
  assert.equal(clearExpiredRequestSession(config, storage), "reviewer");
  assert.equal(storage.getItem("reviewerId"), null);
  assert.equal(storage.getItem("studentId"), "1");
  assert.equal(storage.getItem("adminToken"), "admin-secret");
});

test("admin expiry does not erase student or reviewer accounts", () => {
  const storage = sessions();
  const config = attachRoleToken({ headers: {} }, "admin", storage);
  assert.equal(clearExpiredRequestSession(config, storage), "admin");
  assert.equal(storage.getItem("admin"), null);
  assert.equal(getRoleToken("student", storage), "student-secret");
  assert.equal(getRoleToken("reviewer", storage), "reviewer-secret");
});

test("login requests never attach an unrelated role token or trigger logout", () => {
  const storage = sessions();
  const req = attachRoleToken({
    headers: { Authorization: "Bearer outdated-token" },
    skipAuth: true,
  }, "student", storage);
  assert.equal(req.headers.Authorization, undefined);
  assert.equal(req._sessionRole, null);
  assert.equal(clearExpiredRequestSession(req, storage), null);
  assert.equal(getRoleToken("student", storage), "student-secret");
});

test("stale in-flight HTTP 401 cannot clear a newer login", () => {
  const storage = sessions();
  const req = attachRoleToken({ headers: {} }, "student", storage);
  storage.setItem("token", "newly-issued-student-token");
  assert.equal(clearExpiredRequestSession(req, storage), null);
  assert.equal(getRoleToken("student", storage), "newly-issued-student-token");
});

test("role-aware shared requests honor the last explicitly selected role", () => {
  const storage = sessions();
  setActiveProfileRole("reviewer", storage);
  assert.equal(getSharedAuthRole(storage), "reviewer");
  const reviewer = attachRoleToken({ headers: {} }, getSharedAuthRole(storage), storage);
  assert.equal(reviewer.headers.Authorization, "Bearer reviewer-secret");
  setActiveProfileRole("student", storage);
  const student = attachRoleToken({ headers: {} }, getSharedAuthRole(storage), storage);
  assert.equal(student.headers.Authorization, "Bearer student-secret");
});

test("legacy sessions are deterministic and support only-reviewer users", () => {
  const storage = sessions();
  assert.equal(getSharedAuthRole(storage), "student");
  clearRoleSession("student", storage);
  assert.equal(getSharedAuthRole(storage), "reviewer");
  clearRoleSession("reviewer", storage);
  assert.equal(getSharedAuthRole(storage), null);
});

test("unauthenticated requests never clear a role on HTTP 401", () => {
  const storage = sessions();
  const req = attachRoleToken({ headers: {} }, null, storage);
  assert.equal(clearExpiredRequestSession(req, storage), null);
  assert.equal(storage.getItem("token"), "student-secret");
});

test("logout only removes selected role and selected profile marker", () => {
  const storage = sessions();
  setActiveProfileRole("student", storage);
  clearRoleSession("admin", storage);
  assert.equal(getSharedAuthRole(storage), "student");
  clearRoleSession("student", storage);
  assert.equal(getSharedAuthRole(storage), "reviewer");
  assert.equal(storage.getItem("theme"), "dark");
});

test("invalid roles never change saved active role", () => {
  const storage = sessions();
  setActiveProfileRole("reviewer", storage);
  assert.throws(() => setActiveProfileRole("admin", storage), /Invalid profile role/);
  assert.equal(getSharedAuthRole(storage), "reviewer");
});
