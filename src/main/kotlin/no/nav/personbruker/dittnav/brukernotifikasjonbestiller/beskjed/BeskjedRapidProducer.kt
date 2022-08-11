package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.enableDittNavJsonConfig
import org.apache.kafka.clients.producer.ProducerRecord

class BeskjedRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, String>,
    private val topicName: String
) {
    fun produce(beskjeder: List<Beskjed>) {
        val objectMapper = ObjectMapper()
        objectMapper.enableDittNavJsonConfig()
        beskjeder.forEach {
            val objectNode = objectMapper.valueToTree<ObjectNode>(it)
            objectNode.put("@event_name", "beskjed")
            val producerRecord = ProducerRecord(topicName, it.eventId, objectNode.toString())
            kafkaProducer.send(producerRecord)
        }
    }
}