# Collaboration application status validation

**Scope:** `PATCH /collabapplication/updateStatus/{id}?status=ACCEPTED|REJECTED`

## Security contract

Only the owner of the collaboration may transition one of its applications
from `PENDING` to `ACCEPTED` or `REJECTED`. The decision is terminal:

| Current status | Requested status | Result |
| --- | --- | --- |
| PENDING | ACCEPTED | 200; saved ACCEPTED |
| PENDING | REJECTED | 200; saved REJECTED |
| PENDING | PENDING | 400; initial state cannot be requested |
| PENDING | any other value, blank or missing | 400 |
| ACCEPTED | ACCEPTED or REJECTED | 409; unchanged |
| REJECTED | ACCEPTED or REJECTED | 409; unchanged |
| PENDING | valid decision by a non-owner | 403; unchanged |
| (no matching application) | valid decision | 404 |

The endpoint retains its existing request format for frontend compatibility.
Case and outer whitespace in owner decisions are normalized. Requests with
unrecognized state strings are never persisted.

A `PESSIMISTIC_WRITE` row lock inside a `@Transactional` service method
prevents concurrent decisions on the same application from both succeeding.
The second writer should observe the terminal status and receive HTTP 409.

## Migration and verification

The entity's existing string `status` field remains unchanged: no new
schema columns are required. Before rollout, inspect existing status values
and agree on a data-cleanup plan for legacy values outside
`PENDING`, `ACCEPTED`, and `REJECTED`; this change intentionally refuses
to transition those records instead of guessing.

Run `CollaborationApplicationStatusTests` with Maven/JUnit and verify the
HTTP behavior using authorized staging accounts, including a simultaneous
accept-versus-reject race. Check the database record remains in exactly one
terminal state and the frontend reloads on a conflict.

**Testing caveat:** `HubBackEnd/pom.xml` currently specifies
`<skipTests>true</skipTests>`. Enable test execution before considering any
build green. This patch is committed as source code and is not a production
pentest or a verified deployment.
