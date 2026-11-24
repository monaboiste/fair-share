# Backend boundaries

- `domain`: Group settings, invariants, and domain events. No project dependencies.
- `application`: Group commands and queries, revision checks, and persistence port. Depends on `domain` only.
- `infrastructure`: REST adapter (`GroupApi`, controller, wire models and global Problem Details advice) and the
  SQLite Group Store. Depends on `application` and `domain`.
- `bootstrap`: Spring Boot startup and separate REST and persistence configurations. Depends on `application` and
  `infrastructure`.
- `acceptance-tests`: boots actual backend instances against a shared local database and exercises the public HTTP API.

The Rules archetype's explicit predicates become typed Group Name and Settlement currency values and a named Group
revision precondition. Bean Validation handles required request fields; business rules remain in the Group slice.
The Accounting archetype's transaction boundary is one Group: check the revision before changing settings and compare
it again when committing an ordered, nonempty batch of events. Either the whole batch commits or none does. The current
commands emit one named event each. No Accounts or postings exist in this slice. Settlement currency is always unlocked
until Expense recording adds the permanent lock.

## Storage

| Property                                            | Default                     | Purpose                                      |
| --------------------------------------------------- | --------------------------- | -------------------------------------------- |
| `fair-share.persistence.sqlite.jdbc-url`            | `jdbc:sqlite:fair-share.db` | Durable database file                        |
| `fair-share.persistence.sqlite.busy-timeout-millis` | `5000`                      | Maximum wait for SQLite locks                |
| `fair-share.persistence.sqlite.synchronous`         | `EXTRA`                     | Durable SQLite sync mode (`EXTRA` or `FULL`) |

Prefer `fair-share.persistence.sqlite.jdbc-url`. On upgrade, `fair-share.database-url` remains a fallback when the
new setting is absent. Set all backend processes to the same absolute database path on a local filesystem with
SQLite-compatible locks. Independent files, network filesystems and multiple hosts are not supported. Preserve the file
across restarts. Each read replays one committed stream directly from SQLite; there is no cache. SQLite serializes write
transactions; revisions and event order are scoped to each Group.

Stop old backend processes before upgrading; the new schema does not support old writers. Startup migrates the
legacy unversioned table to schema 1 in one transaction, preserving Group IDs, event positions,
and revisions. Failed migrations and unsupported schema versions prevent startup. Rows carry a stable event type ID
and format version independent of Kotlin class names. An unsupported event kind or format makes that stream unreadable;
the backend never skips recorded events. See the [SQLite ADR](adr/0001-sqlite-for-group-event-storage.md).

## Problem Details

Set `fair-share.rest.problem-origin` to the stable public documentation origin, for example `https://api.example.org`
(replaces `fair-share.problem-origin`). The local default is `http://localhost:8080`. Route `/problems/*` at that origin
to the backend so clients can dereference type URIs. Request headers do not affect the URIs. Responses use
`application/problem+json`; branch on `type` and field `code`, not `detail`.

| Status | Type path                          | Meaning                                                                                                         |
| ------ | ---------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 400    | `/problems/invalid-request`        | Malformed or unsupported JSON shape, or `If-Match` is not one strong entity tag.                                |
| 404    | `/problems/group-not-found`        | Addressed Group does not exist.                                                                                 |
| 412    | `/problems/group-version-mismatch` | Tag does not match at load or append. This request did not commit.                                              |
| 422    | `/problems/invalid-group`          | Structurally valid settings violate Group rules.                                                                |
| 428    | `/problems/precondition-required`  | Supply the Group's current ETag in `If-Match`.                                                                  |
| 500    | `/problems/internal-error`         | Internal failure or unknown write outcome. A write may have committed. Refresh and inspect before resubmitting. |

Field errors include `pointer`, `code`, and `detail`. Pointers are RFC 6901 URI fragments (`#/settlementCurrency`);
missing fields point to the containing object (`#`). Stable codes for this slice:

- `INVALID_JSON`: body could not be read as exactly one JSON value with unique field names.
- `REQUIRED_FIELD`: required body or field absent.
- `INVALID_TYPE`: expected JSON object or string, not another type or explicit null.
- `UNSUPPORTED_FIELD`: field not accepted by the operation.
- `BLANK_DISPLAY_NAME`: display name contains no non-whitespace characters.
- `INVALID_SETTLEMENT_CURRENCY`: use a supported uppercase ISO 4217 code with standard minor units.
