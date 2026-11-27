# Persist explicitly versioned Protobuf event payloads

Use binary Protocol Buffers for durable events, independent of REST JSON and Kotlin domain types. Persistence-only
messages make the stored contract explicit and let domain and HTTP models evolve independently. This adds schema and
build tooling but avoids tying durable history to class names, REST configuration, or Protobuf's wire-compatibility
rules.

## Payload contract

Keep stable type identifiers, per-type positive versions starting at `1`, stream identity, and recording metadata
outside the payload. Dispatch explicitly by `(eventType, eventVersion)`. Use proto3 with explicit field presence and a
separate message definition for each version. The first Group payloads are:

| Event type                    | Version | Fields                                                                        |
| ----------------------------- | ------- | ----------------------------------------------------------------------------- |
| `group-created`               | `1`     | `optional string display_name = 1`, `optional string settlement_currency = 2` |
| `settlement-currency-changed` | `1`     | `optional string settlement_currency = 1`                                     |

All listed fields must be present. Preserve the accepted display name unchanged and store the Settlement currency code.
Reject unsupported type/version pairs, malformed binary data, unknown fields, missing required values, and invalid
recorded values. Keep standard Protobuf duplicate-field semantics, including last value wins for these scalar fields;
canonical binary encoding is not required.

Freeze released message definitions and semantics. Any structural or semantic change, including a wire-compatible
addition, gets a new message and payload version. Write the current version and retain explicit readers for all released
versions. Compatibility means both decoding old bytes and reconstructing state with the original facts' meaning.
Construct domain values and validate historical facts during replay; never apply today's command acceptance rules to old
facts. Tightening a command rule must not strand previously valid history. No history rewrites or generic upcasting
framework. Forward compatibility, mixed-version deployments, and rollback after writing a version the old binary cannot
read are unsupported.

## Persistence and failure boundary

Keep the existing `EventCodec` operations. `EncodedEvent` owns bytes independently of Protobuf types: copy mutable
inputs and exposed bytes, and use content equality and hashing if retaining value semantics. Bind and read the payload
as binary in JDBC, without Base64. Encode the full batch before executing append SQL. The append participates in the
application's write transaction as specified in
[ADR 0002](0002-application-use-cases-with-explicit-failure-semantics-and-conditional-event-commits.md). Decode failures
use `IllegalArgumentException`; SQLite reports `InvalidEventHistoryException` without partial replay, preserving the
cause and recording stream ID, sequence, event ID, and type/version where available. Keep raw payloads out of
diagnostics and return only the existing generic internal error to HTTP clients.

Keep the original Liquibase changeset unchanged. Add an explicit, offline, transactional migration from SQLite schema
version `1` to `2`. Require both event tables to be empty; otherwise halt without changing the event schema or data.
Change `event.payload` from `TEXT NOT NULL` to `BLOB NOT NULL`, preserving all other constraints and `STRICT` semantics.
Set `PRAGMA user_version = 2` in the same transaction. Reapplying the payload migration is a no-op that preserves data.
No legacy payload conversion. SQLite schema and event payload versions evolve independently. Startup and Group view
rebuild require the exact schema version of the running application, including any subsequent view migrations.

## Build and compatibility checks

Apply Google's Protobuf Gradle plugin through a scoped persistence convention used only by `infrastructure`. Generate
full Java messages into a dedicated package for Kotlin to consume; no Kotlin message DSL or checked-in generated code.
Pin the plugin and `protoc` in the version catalog, aligning the compiler with the full `protobuf-java` runtime resolved
from the Spring Boot BOM. Scope any applicable coverage, mutation, formatting, or static analysis exclusions to
generated code only. Keep existing thresholds and handwritten code checks.

Check in independently produced binary fixtures for every released type/version, with expected contents and provenance
in textproto or a README. Never regenerate fixtures during tests or require exact writer bytes. Cover fixture decoding,
current round trips, each rejection case, duplicate semantics, and defensive byte ownership. Assert reconstructed state
from historical fixtures, including facts that newer command rules no longer accept when such a rule changes. At the
SQLite boundary, exercise fresh provisioning, empty v1 upgrade, repeat migration at v2, nonempty v1 refusal, and replay
after reopening the database with the real codec.

Decision: [Durable event serialization and evolution](https://github.com/monaboiste/fair-share/issues/30).
