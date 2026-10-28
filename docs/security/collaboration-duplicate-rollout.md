# Collaboration application duplicate protection — deployment notes

This change prevents repeated applications to the same team. It is implemented
in the **backend**, independent of frontend disabled buttons:

- A transaction locks the collaboration row before checking existing applications.
- The backend returns HTTP **409 Conflict** for an existing application, a
  self-application, or a team no longer accepting applications.
- A database uniqueness constraint backs up the application-layer check.
- An owner notification is issued only after a successful insert/flush.
- The frontend displays the API error and refreshes the student's applications
  after a conflict.

## Existing databases: required preflight

**Do not deploy the new unique constraint blindly on a database with existing
duplicate records.** First back up the database and verify the actual table
and column names in your environment (Spring/Hibernate physical naming may
transform the JPA name `collabApplication` to `collab_application`):

```sql
SHOW TABLES LIKE 'collab%';
SHOW CREATE TABLE collab_application;

SELECT collaboration_id, student_id, COUNT(*) AS duplicate_count
FROM collab_application
GROUP BY collaboration_id, student_id
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS incomplete_applications
FROM collab_application
WHERE collaboration_id IS NULL OR student_id IS NULL;
```

If duplicates are found, have the product owner review their statuses
(`PENDING`, `ACCEPTED`, `REJECTED`) and decide which record to preserve.
**Do not automatically delete duplicates** because this can erase accepted
applications or the wrong student's history.

After backing up and resolving duplicates, confirm the unique key is present
in staging. If schema migration tooling has not installed it, use a reviewed
MySQL migration (adjust table/column names to match `SHOW CREATE TABLE`):

```sql
ALTER TABLE collab_application
ADD CONSTRAINT uk_collab_application_team_student
UNIQUE (collaboration_id, student_id);
```

Avoid executing the migration twice. In production, use a versioned migration
tool and `DDL_AUTO=validate` (or `none`) once migrations are established;
relying on `ddl-auto=update` is not a safe substitute for rollout planning.

## Verification

With two student accounts and one test team on an **authorized test
environment**:

1. Student A creates a team and sets it to accept applications.
2. Student B applies once: API returns success; one application and one
   owner notification exist.
3. Student B applies again: API returns HTTP 409 and does not create another
   application or notification.
4. Student A cannot apply to their own team (HTTP 409).
5. For a team with `status=false`, a new application is rejected (HTTP 409).
6. Two simultaneous applications from B should result in a single saved
   record, with the other request rejected; verify this with a real DB
   integration test before production.
7. Confirm other endpoints for the same student still list the existing
   application with its original status.

Unit tests: `CollaborationApplicationDuplicateTests`.

**Testing caveat:** The repository's Maven Surefire configuration currently
contains `<skipTests>true</skipTests>`; do not interpret a green Maven package
build as proof that these tests ran. Reconfigure that setting or use a
dedicated test profile before merging. No production testing is part of this
change.
