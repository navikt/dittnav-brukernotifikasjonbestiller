package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.concurrent.TimeUnit

class VarselRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log = KotlinLogging.logger { }

    fun produce(varsel: Varsel) {
        val objectNode = objectMapper.valueToTree<ObjectNode>(varsel)
        objectNode.put("@event_name", varsel.type.eventtype)
        val producerRecord = ProducerRecord(topicName, varsel.eventId, objectNode.toString())
        kafkaProducer.send(producerRecord).also {
            try {
                it.get(5, TimeUnit.SECONDS)
                log.info("Videresendt validert ${varsel.type.eventtype} til intern-topic: ${varsel.eventId}")

            } catch (e: Exception) {
                log.warn { "Feil i produsering av ${varsel.type.eventtype} med eventid ${varsel.eventId}: ${e.message}" }
                throw e
            }

        }
    }

    fun flushAndClose() {
        try {
            kafkaProducer.flush()
            kafkaProducer.close()
            log.info("Produsent for kafka-eventer er flushet og lukket.")
        } catch (e: Exception) {
            log.warn("Klarte ikke å flushe og lukke produsent. Det kan være eventer som ikke ble produsert.")
        }
    }
}