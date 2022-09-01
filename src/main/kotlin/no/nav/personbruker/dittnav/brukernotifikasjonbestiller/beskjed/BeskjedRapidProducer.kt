package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BeskjedRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    fun produce(beskjed: Beskjed) {
        val objectNode = objectMapper.valueToTree<ObjectNode>(beskjed)
        objectNode.put("@event_name", "beskjed")
        val producerRecord = ProducerRecord(topicName, beskjed.eventId, objectNode.toString())
        kafkaProducer.send(producerRecord)
        log.info("Produsert beskjed på rapid med eventid ${beskjed.eventId}")

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