# Use Group revision ETags and return the verified snapshot for no-op writes

Keep HTTP validators in `infrastructure`. One shared codec encodes the canonical Group ID and positive application
`GroupRevision` in a quoted, versioned strong ETag (`"g1.<base64url-id>.<revision>"`). One canonical encoding per pair
keeps tags stable across instances and restarts without keys, persisted tokens, or a dependency on `StreamRevision`.
Clients echo tags verbatim; they are not credentials. Change the tag version if Group HTTP representation changes would
otherwise make the same strong tag identify different representation data at the same revision.

Require a single supported strong `If-Match` on existing-Group writes. Validate request structure and intrinsic inputs
before testing the match. A recognized tag for another Group never satisfies the precondition, even if its numeric
revision matches. The application still verifies history and checks the expected revision before deciding, and a
nonempty batch must pass the conditional append. Use standard MVC revalidation for `GET /groups/{groupId}` only after
the query checks the current committed view. Do not use the validator to bypass a fresh read or suppress consistency
failures.

Setting the same Settlement currency is an accepted empty decision, including after the currency is locked. After
verifying the loaded snapshot and expected revision, application returns that snapshot and its revision without an
append, view update, event metadata, or write transaction. No extra revision read or lock is needed: a competing write
may commit later and return the next revision, even before the no-op response is sent. Both requests can succeed, but
only the write commits. Unlike a synthetic event or empty write transaction, this keeps history and revisions tied to
recorded facts. An already stale request still fails.

Decision: [HTTP adapter conventions and Group revisions](https://github.com/monaboiste/fair-share/issues/33).
