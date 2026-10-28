# Authentication session isolation

This patch keeps the existing student, reviewer, and administrator token keys
for compatibility, but isolates request authentication and expired-session
cleanup by role.

## Behavior

- Student requests use only `token`.
- Reviewer requests use only `reviewerToken`.
- Administrator requests use only `adminToken`.
- Profile requests use the most recently selected student/reviewer role,
  saved under `activeProfileRole`. Callers can explicitly set `authRole`
  per request. The previous `studentToken || reviewerToken` precedence is
  removed.
- Authentication forms send `skipAuth: true`, so an old student's access
  token is never attached to faculty or student login submissions.
- A 401 expires *only the token that was actually sent in that request*,
  and only if it still matches localStorage. A delayed 401 for an old token
  cannot delete a fresh login.
- Explicit logout removes the selected role's metadata and tokens but
  preserves other roles, UI preferences, and unrelated localStorage values.
- 403 authorization failures do not automatically log the user out.
- Public pages do not force navigation to the login page when a role-specific
  background request fails.

## Verification

From `HubFrontend` run:

```bash
npm run test:auth
npm run build
```

The included `node:test` suite checks role isolation, logout behavior,
unrelated localStorage preservation, skipped authentication, stale 401s,
explicit role selection, and legacy sessions. To test Axios behavior in a
browser, use *authorized test accounts* with the following checks:

1. Log in as student, then as reviewer in the same browser. Verify the
   shared profile client uses the last selected role.
2. Request the student login API while an expired reviewer or admin token
   exists; ensure no token is attached to the login request.
3. Expire a student's token, trigger an authorized student-only endpoint,
   and confirm the reviewer/admin tokens remain untouched.
4. Repeat for reviewer and administrator tokens.
5. Log out of each role separately, verifying that the other two sessions
   and site preferences survive.
6. Generate an in-flight request with an old token and log in again before
   its 401 returns; the newer token must remain untouched.
7. A valid token lacking permission should produce HTTP 403, without
   clearing that role's session.

## Limits

These fixes address cross-role client state. Browser localStorage remains
readable to JavaScript running in the origin, so preventing XSS is essential.
For stronger long-term session protection, evaluate secure HttpOnly cookies
with appropriate CSRF defenses, short-lived tokens, and server-side revocation
rather than treating client-side removal as token revocation.

The source change is not automatically deployed and does not prove all
backend auth/authorization checks are correct. Review and run the entire
application build and regression suite before merging.
