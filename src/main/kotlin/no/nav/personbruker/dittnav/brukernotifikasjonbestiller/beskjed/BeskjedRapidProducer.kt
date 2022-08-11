package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import org.apache.kafka.clients.producer.ProducerRecord

class BeskjedRapidProducer(
    private val kafkaProducer: org.apache.kafka.clients.producer.Producer<String, Beskjed>,
    private val topicName: String
) {
    fun produce(beskjeder: List<Beskjed>) {
        beskjeder.forEach {
            val producerRecord = ProducerRecord(topicName, it.eventId, it)
            kafkaProducer.send(producerRecord)
        }
    }
}