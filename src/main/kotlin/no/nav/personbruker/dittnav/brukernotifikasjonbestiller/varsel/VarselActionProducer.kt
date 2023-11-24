package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

class VarselActionProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    private val log = KotlinLogging.logger { }

    fun produce(outputEvent: OutputEvent) {
        val producerRecord = ProducerRecord(topicName, outputEvent.varselId, outputEvent.content)
        kafkaProducer.send(producerRecord).also {
            try {
                it.get(5, TimeUnit.SECONDS)
                log.info { "Videresendt '${outputEvent.action}'-event til nytt topic." }

            } catch (e: Exception) {
                log.warn { "Feil i produsering av '${outputEvent.action}'-event" }
                throw e
            }
        }
    }

    fun flushAndClose() {
        try {
            kafkaProducer.flush()
            kafkaProducer.close()
            log.info { "Produsent for kafka-eventer er flushet og lukket." }
        } catch (e: Exception) {
            log.warn { "Klarte ikke å flushe og lukke produsent. Det kan være eventer som ikke ble produsert." }
        }
    }
}


data class OutputEvent(
    val action: String,
    val varselId: String,
    val content: String
)
