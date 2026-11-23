# Backend boundaries

- `domain`: Group settings, invariants, and domain events. No project dependencies.
- `application`: Group commands and queries, revision checks, and persistence port. Depends on `domain` only.
- `infrastructure`: Spring MVC, JSON and Problem Details, and the SQLite Group Store. Depends on `application` and
  `domain`.
- `bootstrap`: Spring Boot startup, configuration, and wiring. Depends on `application` and `infrastructure`.

The Rules archetype's explicit predicates become typed Group settings and preconditions, not a rules engine. The
Accounting archetype's transaction boundary becomes one Group: validate a revision before mutation, then append the
whole operation as one durable event if that revision is still current. No Accounts or postings exist in this slice.
Settlement currency is always unlocked until Expense recording adds the permanent lock.

## Storage

`fair-share.database-url` defaults to `jdbc:sqlite:fair-share.db`. For multiple backend processes, set each to the same
absolute database path on a local filesystem with SQLite-compatible locks. Independent files, network filesystems and
multi-host deployments are not supported by this adapter. Use a writable directory and preserve the database across
restarts. Each read replays one committed stream directly from SQLite; there is no cache. SQLite serializes write
transactions internally, but revision checks and event order are scoped to each Group.

## Problem Details

Set `fair-share.problem-origin` to the stable public documentation origin, for example `https://api.example.org`. The
default for local development is `http://localhost:8080`. Route `/problems/*` at that origin to the backend so clients
can dereference type URIs. Request headers do not affect the URIs. Responses use `application/problem+json`; branch on
`type` and field `code`, not `detail`.

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
