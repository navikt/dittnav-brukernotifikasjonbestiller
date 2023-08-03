package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord

class DoneRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log = KotlinLogging.logger {  }

    fun produce(done: Done) {
        val objectNode = objectMapper.valueToTree<ObjectNode>(done)
        objectNode.put("@event_name", "done")
        val producerRecord = ProducerRecord(topicName, done.eventId, objectNode.toString())
        kafkaProducer.send(producerRecord)
        log.info { "Videresendt validert done til intern-topic: ${done.eventId}" }
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
