package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.errors.SerializationException

/**
 * Avro deserialiserer som returnerer `null` hvis den mottar bytes som ikke kan deserialiseres til en Avro-type.
 *
 * Denne implementasjonen er laget for å kunne ignorere mottatte eventer som ikke lar seg deserialisere, det vil
 * bare logges at et event ikke lot seg deserialisere og det returneres `null`. Dette gjør at appens pollere ikke
 * stopper selv om det skulle komme et event som ikke lar seg deserialisere.
 */
class SwallowSerializationErrorsAvroDeserializer : KafkaAvroDeserializer {
    constructor() : super()
    constructor(schemaRegistryClient: SchemaRegistryClient) : super(schemaRegistryClient)
    constructor(schemaRegistryClient: SchemaRegistryClient, pros: MutableMap<String, Any>) : super(
        schemaRegistryClient,
        pros
    )

    private val log = KotlinLogging.logger { }

    override fun deserialize(bytes: ByteArray): Any? {
        var result: Any? = null
        try {
            result = super.deserialize(bytes)

        } catch (e: SerializationException) {
            val msg =
                "Eventet kunne ikke deserialiseres, og blir forkastet. Dette skjedde mest sannsynlig fordi eventet ikke var i henold til Avro-skjemaet for denne topic-en."
            log.error(e) { msg }
        }
        return result
    }

}
