package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.DependentTransactionException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import org.slf4j.LoggerFactory

class Producer<K, V>(
        private val destinationTopicName: String,
        private val kafkaProducer: KafkaProducer<K, V>
) {

    val log = LoggerFactory.getLogger(Producer::class.java)

    suspend fun sendEventsTransactionally(events: List<RecordKeyValueWrapper<K, V>>, dependentTransaction: suspend () -> Unit = {}) {
        try {
            kafkaProducer.beginTransaction()
            events.forEach { event ->
                sendSingleEvent(event)
            }
            executeDependentTransaction(dependentTransaction)
            kafkaProducer.commitTransaction()
        } catch (e: DependentTransactionException) {
            kafkaProducer.abortTransaction()
            throw RetriableKafkaException("Transaksjon mot kafka er avbrutt grunnet feil i annen transaksjon", e)
        } catch (e: KafkaException) {
            kafkaProducer.abortTransaction()
            throw RetriableKafkaException("Et eller flere eventer feilet med en periodisk feil ved sending til kafka", e)
        } catch (e: Exception) {
            kafkaProducer.close()
            throw UnretriableKafkaException("Fant en uventet feil ved sending av eventer til kafka", e)
        }
    }

    private fun sendSingleEvent(event: RecordKeyValueWrapper<K, V>) {
        val producerRecord = ProducerRecord(destinationTopicName, event.key, event.value)
        kafkaProducer.send(producerRecord)
    }

    private suspend fun executeDependentTransaction(dependentTransaction: suspend () -> Unit) {
        try {
            dependentTransaction()
        } catch (e: Exception) {
            throw DependentTransactionException("Feil ved håndtering av transaksjon", e)
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
