package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.slf4j.LoggerFactory

class Producer<K, V>(
        private val destinationTopicName: String,
        private val kafkaProducer: org.apache.kafka.clients.producer.Producer<K, V>
) {

    val log = LoggerFactory.getLogger(Producer::class.java)

    fun sendEventsAndLeaveTransactionOpen(events: List<RecordKeyValueWrapper<K, V>>) {
        try {
            kafkaProducer.beginTransaction()
            events.forEach { event ->
                sendSingleEvent(event)
            }
        } catch (e: KafkaException) {
            kafkaProducer.abortTransaction()
            throw RetriableKafkaException("Et eller flere eventer feilet med en periodisk feil ved sending til kafka", e)
        } catch (e: Exception) {
            kafkaProducer.close()
            throw UnretriableKafkaException("Fant en uventet feil ved sending av eventer til kafka", e)
        }
    }

    fun sendEventsTransactionally(events: List<RecordKeyValueWrapper<K, V>>) {
        try {
            kafkaProducer.beginTransaction()
            events.forEach { event ->
                sendSingleEvent(event)
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

    fun abortCurrentTransaction() {
        try {
            kafkaProducer.abortTransaction()
        } catch (e: Exception) {
            /**/
        }
    }

    fun commitCurrentTransaction() {
        kafkaProducer.commitTransaction()
    }

    private fun sendSingleEvent(event: RecordKeyValueWrapper<K, V>) {
        val producerRecord = ProducerRecord(destinationTopicName, event.key, event.value)
        kafkaProducer.send(producerRecord)
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
