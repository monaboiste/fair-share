package fairshare.infrastructure.persistence.sqlite.events

import fairshare.domain.events.DomainEvent
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

internal data class TestCreated(val name: String) : DomainEvent

internal data class TestRenamed(val name: String) : DomainEvent

internal class TestEventCodec : EventCodec {
    private val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build()

    override fun encode(event: DomainEvent): EncodedEvent {
        val type = when (event) {
            is TestCreated -> "test.created"
            is TestRenamed -> "test.renamed"
            else -> throw IllegalArgumentException("Unsupported test event: $event")
        }
        return EncodedEvent(type, version = 1, payload = mapper.writeValueAsString(event))
    }

    override fun decode(event: EncodedEvent): DomainEvent {
        require(event.version == 1) { "Unsupported test event version: ${event.version}" }
        val payloadClass = when (event.type) {
            "test.created" -> TestCreated::class.java
            "test.renamed" -> TestRenamed::class.java
            else -> throw IllegalArgumentException("Unsupported test event type: ${event.type}")
        }
        val payload: DomainEvent? = try {
            mapper.readValue(event.payload, payloadClass)
        } catch (exception: JacksonException) {
            throw IllegalArgumentException("Malformed ${event.type} payload", exception)
        }
        return requireNotNull(payload) { "Missing ${event.type} payload" }
    }
}
