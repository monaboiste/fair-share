# Fair Share backend

## Group API

| Operation                          | Input                               | Successful response      |
| ---------------------------------- | ----------------------------------- | ------------------------ |
| `POST /groups`                     | `displayName`, `settlementCurrency` | `201`, `Location`, Group |
| `GET /groups/{groupId}`            | Opaque Group ID                     | `200`, Group             |
| `PATCH /groups/{groupId}/currency` | `settlementCurrency`, `If-Match`    | `200`, updated Group     |

Every successful response includes a strong opaque Group `ETag`. Send exactly one strong tag in `If-Match` when changing
currency. On a stale tag, refresh the Group before deciding whether to resubmit. A Group ID identifies a resource, not
an account or credential.

```json
{
  "groupId": "some-group-id",
  "displayName": "Weekend trip",
  "settlementCurrency": "PLN",
  "participants": []
}
```

Currency changes are permitted in this slice because Expense recording does not exist yet. #16 will prohibit changes
from the first successfully recorded Expense onward, even if that Expense is later reversed. The decision will use
recorded Expense history, not the currently effective Expense list or a separate setting or event.

## Boundaries

- `domain`: Group state, value objects, business rules, and domain events. The Rules archetype is visible in
  `SettlementCurrencyRule`: Settlement requires a supported uppercase currency code with standard minor units from
  Joda-Money. `GroupName` rejects blank names. Both invariants hold at construction.
- `application`: command use cases load recorded events, check the expected revision, reconstruct the Group and append
  domain events under that revision. Revision checking is an application precondition, not a business rule. The query
  use case uses a separate Group view port and does not replay the aggregate.
- `infrastructure`: Spring MVC request binding, validation and Problem Details; SQLite event store and Group view
  adapters. The view validates recorded history and derives data and the revision in a single database snapshot.
- `bootstrap`: application wiring, validated external properties, REST JSON customization and startup schema
  initialization.
- `acceptance-tests`: runs the current packaged `bootJar` in a separate JVM and tests organizer journeys through HTTP.
  It does not compile against production modules. Technical HTTP and SQLite behavior are tested at their own boundaries.

The Group is the consistency boundary for future Accounting operations. Event storage and its SQL transaction are not
Accounting transactions. This slice does not model Accounts, Postings, Balances or Accounting transaction constraints.

## SQLite

| Property                                            | Default                     | Purpose                                      |
| --------------------------------------------------- | --------------------------- | -------------------------------------------- |
| `fair-share.persistence.sqlite.jdbc-url`            | `jdbc:sqlite:fair-share.db` | Durable database file                        |
| `fair-share.persistence.sqlite.busy-timeout-millis` | `5000`                      | Maximum wait for SQLite locks                |
| `fair-share.persistence.sqlite.synchronous`         | `EXTRA`                     | Durable SQLite sync mode (`EXTRA` or `FULL`) |

All backend instances must share the same durable local SQLite database file with SQLite-compatible locking. Separate
files, network filesystems and multiple hosts are unsupported. Preserve the database file across restarts.

The schema initializer creates the current table idempotently before serving requests. Development schemas are not
backward compatible: incompatible files fail startup rather than being migrated or replaced. To start fresh, configure
a new database path. Liquibase is preferred for future schema management but deferred beyond #14. See
[ADR 0001](adr/0001-sqlite-for-group-event-storage.md).

Events have stable unique IDs, per-Group sequence numbers, recorded timestamps and a Group revision. Sequence, not
timestamp, controls replay order. A successful conditional append advances the Group revision and commits the whole
batch together. Events in that batch share the new revision but keep separate IDs. A read starting after success sees
that commit or a newer one, including on a different instance. No write is retried against a newer revision
automatically.

Stable event types `group-created` and `settlement-currency-changed` use encoding version `1`. Encoding versions are not
database schema versions. Unsupported encodings and invalid recorded history fail reads rather than yielding partial
data.

## Problem Details

Set `fair-share.rest.problem-origin` to the stable public HTTP or HTTPS documentation origin, for example
`https://api.example.org`. The local default is `http://localhost:8080`. Route `/problems/*` at that origin to the
backend so clients can dereference type URIs. Incoming hosts do not affect the URIs. Clients receive
`application/problem+json` and should branch on `type` and field `code`, not `detail`.

| Status | Type path                          | Meaning                                                     |
| ------ | ---------------------------------- | ----------------------------------------------------------- |
| 400    | `/problems/invalid-request`        | Malformed or unsupported JSON or precondition.              |
| 404    | `/problems/group-not-found`        | Addressed Group does not exist.                             |
| 412    | `/problems/group-version-mismatch` | Tag mismatch at load or append; no write committed.         |
| 422    | `/problems/invalid-group`          | Input violates Group business rules.                        |
| 428    | `/problems/precondition-required`  | Required `If-Match` was absent.                             |
| 500    | `/problems/internal-error`         | Internal failure, invalid history or unknown write outcome. |

An unknown outcome or lost response may hide a committed write. Refresh and inspect before considering resubmission.

Field errors include `pointer`, `code` and `detail`. Pointers are RFC 6901 URI fragments such as `#/settlementCurrency`.
Missing fields point to the containing object (`#` at the root). Clients should not rely on error ordering or assume
that a response lists every invalid field.

- `INVALID_JSON`: body could not be read as exactly one JSON value with unique field names.
- `REQUIRED_FIELD`: required body or field absent.
- `INVALID_TYPE`: expected JSON object or string, not another type or explicit null.
- `UNSUPPORTED_FIELD`: field not accepted by the operation.
- `BLANK_DISPLAY_NAME`: display name contains no non-whitespace characters.
- `INVALID_SETTLEMENT_CURRENCY`: unsupported Settlement currency code.
