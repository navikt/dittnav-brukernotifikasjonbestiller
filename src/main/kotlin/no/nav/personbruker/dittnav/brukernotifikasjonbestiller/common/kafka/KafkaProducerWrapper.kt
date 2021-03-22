package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException

class KafkaProducerWrapper<K, V>(
        private val destinationTopicName: String,
        private val kafkaProducer: KafkaProducer<K, V>
) {

    fun sendEvents(events: List<RecordKeyValueWrapper<K, V>>) {
        try {
            kafkaProducer.beginTransaction()
            events.forEach { event ->
                sendEvent(event)
            }
            kafkaProducer.commitTransaction()
        } catch (e: KafkaException) {
            kafkaProducer.abortTransaction()
            throw RetriableKafkaException("Et eller flere eventer feilet med en periodisk feil ved sending til kafka", e)
        } catch (e: Exception) {
            kafkaProducer.close()
            throw UnretriableKafkaException("Fant en uventet feil ved sending av eventer til kafka", e)
        }
    }

    private fun sendEvent(event: RecordKeyValueWrapper<K, V>) {
        val producerRecord = ProducerRecord(destinationTopicName, event.key, event.value)
        kafkaProducer.send(producerRecord)
    }
}
