package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent

/**
 * Serializes events independently of REST. Type identifiers and format versions must be explicit and stable,
 * and decoding an encoded event must recover its original domain data.
 */
internal interface EventCodec {
    /** Encodes an event for storage, or throws [IllegalArgumentException] if its type is unsupported. */
    fun encode(event: DomainEvent): EncodedEvent

    /** Decodes recorded data, throwing [IllegalArgumentException] if the type, version, or payload is invalid. */
    fun decode(event: EncodedEvent): DomainEvent
}
