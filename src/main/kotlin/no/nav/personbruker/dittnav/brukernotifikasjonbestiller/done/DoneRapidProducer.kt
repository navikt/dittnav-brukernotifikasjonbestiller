package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DoneRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    fun produce(doneEventer: List<Done>) {
        doneEventer.forEach {
            val objectNode = objectMapper.valueToTree<ObjectNode>(it)
            objectNode.put("@event_name", "done")
            val producerRecord = ProducerRecord(topicName, it.eventId, objectNode.toString())
            kafkaProducer.send(producerRecord)
            log.info("Produsert done på rapid med eventid ${it.eventId}")
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