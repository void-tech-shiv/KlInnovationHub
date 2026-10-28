# KL Innovation Hub — Security & production readiness review

Scope: fork `void-tech-shiv/KlInnovationHub` and the cumulative fixes
`#01–#08`, reviewed from the branch `fix/security-isolated-auth-sessions`
(commit `91f29b00f330e4753e10d993617f64c6a385d4e6`).
This report is a **source review and release gate**, not evidence that the
production site, database, secrets or infrastructure were tested.

**Release decision: NO-GO until all blocking gates below pass.**

## Verified CI evidence — 28 October 2026

GitHub Actions job logs on the audit branch confirm that the first validation
run (commit `25947ae`, [run 40672579778](https://github.com/void-tech-shiv/KlInnovationHub/actions/runs/40672579778))
actually compiled **160 main Java source files** and **11 test Java files**,
executed **41 backend tests (0 failures, 0 errors, 0 skipped)** including a
Spring context test using temporary MySQL, and successfully packaged the
backend. Frontend dependency installation, **10 auth session tests**, and
Vite production compilation also passed.

The initial ESLint job failed on **47 errors and 10 warnings**. Follow-up
frontend cleanup reduced this count across repeated CI runs. Commit
`10541d4` has a green frontend build/auth test job and a green ESLint job
(with warnings remaining); the backend run on this commit was not yet complete
at the time of this audit update.

These successful checks validate compilation and automated tests—not the
live production API or migrations. Staging manual QA and transport checks
remain mandatory. The audit branch also adds explicit CI jobs that should
**fail** until the production API endpoint is configured with verified HTTPS
and high-severity npm dependency advisories are resolved.



## What was inspected

- Spring Boot security filter chain, JWT filter, login/OTP and rate-limiting
  services, collaboration applications, submission controllers and entities.
- React Axios interceptors, login/logout and protected routes, frontend build
  scripts, and committed production environment configuration.
- The unit and application-context tests present in the repository.
- Database schema update mode and duplicate-application rollout notes.

## Blocking findings

| ID | Severity | Evidence | Required release gate |
|---|---|---|---|
| B-01 | Critical | `HubFrontend/.env.production` points the client API at an **HTTP** Elastic Beanstalk URL. Bearer tokens must not be sent over plaintext. | Provision and verify an HTTPS API endpoint, update `VITE_API_URL`, enforce HTTP->HTTPS, and test requests in browser DevTools. Do not guess the HTTPS hostname or certificate. |
| B-02 | High | `HubBackEnd/src/main/resources/application.properties` defaults `DDL_AUTO` to `update`; #06 declares a unique constraint on applications. Legacy duplicates or mapping changes can prevent the next startup. | Back up production DB, inspect actual table/column names, remove duplicate rows with owner-approved status selection, apply a *versioned* reviewed migration, and use `DDL_AUTO=validate` or `none` for deployment. |
| B-03 | High | The fork originally set Maven Surefire `<skipTests>true</skipTests>`. | Tests must run and pass in CI. This audit branch changes it to false and adds a validation workflow; a green package build alone is not proof of tests unless test reports exist. |
| B-04 | High | `AdminLoginRateLimiterService` and `PasswordResetRateLimiterService` store throttle counters in a single process's memory. | For multi-instance/restart-resistant limits use a shared atomic store (Redis/DB), and verify locking/limits under load. Single-instance deployment must be documented and independently monitored. |
| B-05 | High | Login JWTs are stored in browser localStorage, and explicit logout deletes browser data but does **not** invalidate previously issued JWTs on the server. | Assess XSS, use strict CSP/escaping, and specify token expiry/rotation and revocation policy. Prefer HttpOnly Secure SameSite cookies with CSRF protection if feasible. Test stolen-token and logged-out-token behavior. |
| B-06 | High | CI/staging checks do not prove the #01–#08 fixes work against the live schema and integrations. | Pass backend context + unit tests with isolated MySQL, frontend build/lint/auth tests, and complete role-based staging QA. |
| B-07 | Medium | `SecurityConfig.java` allows the local development CORS origin in every environment and `/mail/**` is broadly public. | Split dev/prod CORS origins and review all public mail endpoints individually with abuse monitoring. |
| B-08 | High | [Dependency-audit job](https://github.com/void-tech-shiv/KlInnovationHub/actions/runs/40673824998) reported **9 high-severity npm advisories** for locked versions of axios, follow-redirects, form-data, nanoid, postcss, react-router/react-router-dom, source-map-js, and vite. | Use reviewed patch/minor upgrades, regenerate and commit the lockfile, rerun npm audit and full frontend test/build/lint checks. Do not force major upgrades without compatibility tests. |

## Implemented in this audit branch

- Enable backend Surefire test execution by default.
- Add `.github/workflows/security-validation.yml` with frontend build,
  auth regression, lint, and Java/Maven tests using an ephemeral MySQL service.
- Route discussion API auth through the same role-isolated session helper as
  other Axios clients (fixes residual `studentToken || reviewerToken` behavior).
- Add the release checklist below and retain all fixes on an isolated branch.

## Executed GitHub Actions release gates

[Latest five-job validation](https://github.com/void-tech-shiv/KlInnovationHub/actions/runs/40673824998)
recorded the following CI outcomes (distinct from staging/production QA):

| Gate | Observed outcome |
|---|---|
| Frontend dependencies, 10 session-auth tests and Vite production build | **PASS** |
| Frontend ESLint | **PASS**, with non-blocking hook dependency warnings |
| Backend Java compilation + 41 tests including MySQL-backed Spring context | **PASS** in [previous complete run](https://github.com/void-tech-shiv/KlInnovationHub/actions/runs/40673613852); latest five-job run may still be completing |
| Production transport configuration | **FAIL**, because committed frontend `.env.production` uses HTTP |
| Production dependency advisory scan | **FAIL**, 9 high-severity vulnerabilities |

Production transport and dependency advisories are **enforced release blockers**
rather than silently ignored. Do not claim release-ready because the three
compile/test/lint jobs passed.

## Verification matrix (requires CI and environment access)

| Gate | Check | Pass criteria |
|---|---|---|
| Frontend install/build | `cd HubFrontend && npm ci && npm run build` | No install or build errors |
| Auth regression | `cd HubFrontend && npm run test:auth` | All assertions pass |
| Frontend lint | `cd HubFrontend && npm run lint` | No lint errors; triage baseline violations |
| Backend tests | `cd HubBackEnd && mvn -B -ntp test` | All tests execute and pass; Spring context uses temporary MySQL |
| Backend package | `cd HubBackEnd && mvn -B -ntp -DskipTests=false package` | Build passes, test reports generated |
| HTTPS & headers | Browser Network + API integration smoke tests | No remote HTTP; auth sent only to trusted HTTPS origin |
| Auth roles | Student, reviewer and admin staging identities | 401/403 handling isolates roles; forbidden endpoints stay forbidden |
| Project CRUD | Allowed owner/non-owner pairs | DTO input ignores controlled IDs, status, likes; only owners delete |
| OTP | Signup, resend, verification, reset | Correct 6-digit OTP, expiration, attempt limits, no grant reuse |
| Applications | Two students, closed/owned team, concurrent requests | Duplicate requests 409, no duplicate notifications; status terminal |
| Database | Verified backup + reviewed migration in staging | Unique constraint active, no duplicate or malformed legacy rows |
| Deployment | Staging smoke, health checks, logs, rollback test | No regression; rollback available |

## Safe merge/release order

1. Run the GitHub Actions workflow and examine actual logs/reports.
2. Fix any failing unit tests, build errors, or lint findings on this branch.
3. Provision HTTPS API endpoint; do not release the existing HTTP Vite build.
4. Back up and reconcile the database, then trial the explicit migration in staging.
5. Complete role-by-role integration tests and concurrency checks.
6. Record JWT secret rotation, admin credential rotation, environment settings
   and an incident rollback plan without writing secret values into Git.
7. After independent review and all mandatory checks passing, mark the draft
   PR ready and merge. Deployment is a separate change requiring approval.

**Do not treat a draft PR, committed tests, or a GitHub branch as production validation.**
