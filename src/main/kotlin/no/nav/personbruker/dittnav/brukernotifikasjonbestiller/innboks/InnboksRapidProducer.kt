package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InnboksRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    fun produce(innboksVarsel: Innboks) {
        val objectNode = objectMapper.valueToTree<ObjectNode>(innboksVarsel)
        objectNode.put("@event_name", "innboks")
        val producerRecord = ProducerRecord(topicName, innboksVarsel.eventId, objectNode.toString())
        kafkaProducer.send(producerRecord)
        log.info("Produsert innboksvarsel på rapid med eventid ${innboksVarsel.eventId}")
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