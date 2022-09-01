package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OppgaveRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    val log: Logger = LoggerFactory.getLogger(Producer::class.java)

    fun produce(oppgave: Oppgave) {
        val objectNode = objectMapper.valueToTree<ObjectNode>(oppgave)
        objectNode.put("@event_name", "oppgave")
        val producerRecord = ProducerRecord(topicName, oppgave.eventId, objectNode.toString())
        kafkaProducer.send(producerRecord)
        log.info("Produsert oppgave på rapid med eventid ${oppgave.eventId}")
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