package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent

/**
 * Serializes events independently of REST. Type identifiers and format versions must be explicit and stable,
 * and decoding an encoded event must recover its original domain data.
 */
internal interface EventCodec {
    /**
     * Encodes persisted domain events.
     *
     * **Implementation requirements:**
     *
     * - Use explicit and stable event type identifiers and format versions.
     * - Keep event serialization independent of REST serialization.
     * - Reject unsupported formats and malformed payloads with
     *   [IllegalArgumentException].
     */
    fun encode(event: DomainEvent): EncodedEvent

    /**
     * Decodes persisted domain events.
     *
     * **Implementation requirements:**
     *
     * - Use explicit and stable event type identifiers and format versions.
     * - Keep event serialization independent of REST serialization.
     * - Reject unsupported formats and malformed payloads with
     *   [IllegalArgumentException].
     */
    fun decode(event: EncodedEvent): DomainEvent
}
