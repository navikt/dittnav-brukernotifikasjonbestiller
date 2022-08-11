package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import com.fasterxml.jackson.databind.ObjectMapper
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
            val producerRecord = ProducerRecord(topicName, it.eventId, objectMapper.writeValueAsString(it))
            kafkaProducer.send(producerRecord)
        }
    }
}