package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent

/**
 * Serializes persisted events independently of REST serialization.
 * Event type identifiers and format versions must be explicit and stable.
 * Reject unsupported event types, unsupported versions, and malformed payloads with [IllegalArgumentException].
 */
internal interface EventCodec {
    fun encode(event: DomainEvent): EncodedEvent

    fun decode(event: EncodedEvent): DomainEvent
}
