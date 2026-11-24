package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent

internal interface EventCodec {
    fun encode(event: DomainEvent): EncodedEvent

    fun decode(event: EncodedEvent): DomainEvent
}
