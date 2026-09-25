# Use caller-supplied Participant identifiers

Participants use caller-supplied `ParticipantId(UUID)` values unique within a Settlement. An add retry with the same
identifier and original name succeeds without a new event, even after rename or removal; reuse with a different original
name is a typed conflict. Removed identifiers remain retired. Display names may repeat and do not define identity.

Unlike ADR-0007's generated Settlement identifiers, caller-supplied Participant identifiers make add retries safe
without creating duplicate Participants. Settlement open keeps generated identifiers; idempotent open remains deferred.
