package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.brukernotifikasjonbestiller.common.exception.RetriableKafkaException
import no.nav.personbruker.brukernotifikasjonbestiller.common.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException

class KafkaProducerWrapper<T>(
        private val destinationTopicName: String,
        private val kafkaProducer: KafkaProducer<Nokkel, T>
) {

    fun sendEvents(events: List<RecordKeyValueWrapper<T>>) {
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

    private fun sendEvent(event: RecordKeyValueWrapper<T>) {
        val producerRecord = ProducerRecord(destinationTopicName, event.key, event.value)
        kafkaProducer.send(producerRecord)
    }
}
