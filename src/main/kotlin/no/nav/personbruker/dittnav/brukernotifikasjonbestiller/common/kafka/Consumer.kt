package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import kotlinx.coroutines.*
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception.RetriableDatabaseException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception.UnretriableDatabaseException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext

class Consumer<K, V>(
    val topic: String,
    val kafkaConsumer: org.apache.kafka.clients.consumer.Consumer<K, V>,
    val eventBatchProcessorService: EventBatchProcessorService<K, V>,
    val job: Job = Job()
) : CoroutineScope {

    private val log = KotlinLogging.logger { }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    suspend fun stopPolling() {
        job.cancelAndJoin()
    }

    fun isCompleted(): Boolean {
        return job.isCompleted
    }

    fun isStopped(): Boolean {
        return !job.isActive
    }

    fun startPolling() {
        launch {
            kafkaConsumer.use { consumer ->
                consumer.subscribe(listOf(topic))

                while (job.isActive) {
                    pollForAndRelayBatchOfEvents()
                }
            }
        }
    }

    private suspend fun pollForAndRelayBatchOfEvents() = withContext(Dispatchers.IO) {
        try {
            val records = kafkaConsumer.poll(Duration.of(100, ChronoUnit.MILLIS))
            if (records.containsEvents()) {
                eventBatchProcessorService.processEvents(records)
                kafkaConsumer.commitSync()
            }
        } catch (re: RetriableKafkaException) {
            log.warn(re) { "Post mot Kafka feilet, prøver igjen senere. Topic: $topic" }
            rollbackOffset()

        } catch (ure: UnretriableKafkaException) {
            log.warn(ure) { "Alvorlig feil ved post mot kafka. Stopper polling. Topic: $topic" }
            stopPolling()

        } catch (rde: RetriableDatabaseException) {
            log.warn(rde) { "Klarte ikke å skrive til databasen, prøver igjen senrere. Topic: $topic" }
            rollbackOffset()

        } catch (ude: UnretriableDatabaseException) {
            log.error(ude) { "Det skjedde en alvorlig feil mot databasen, stopper videre polling. Topic: $topic" }
            stopPolling()

        } catch (re: RetriableException) {
            log.warn(re) { "Polling mot Kafka feilet, prøver igjen senere. Topic: $topic" }
            rollbackOffset()

        } catch (ce: CancellationException) {
            log.info(ce) { "Denne coroutine-en ble stoppet. ${ce.message}" }
        } catch (ie: InterruptedException) {
            log.warn { "Produsering av event til kafka ble avbrutt" }
            rollbackOffset()

        } catch (te: TimeoutException) {
            log.warn { "Produsering av event til kafka timet ut" }
            rollbackOffset()
        } catch (e: Exception) {
            log.error(e) { "Noe uventet feilet, stopper polling. Topic: $topic" }
            stopPolling()
        }


    }

    private fun ConsumerRecords<K, V>.containsEvents() = count() > 0

    private suspend fun rollbackOffset() {
        withContext(Dispatchers.IO) {
            if (kafkaConsumer is KafkaConsumer) {
                kafkaConsumer.rollbackToLastCommitted()
            }
        }
    }

    private fun <K, V> KafkaConsumer<K, V>.rollbackToLastCommitted() {
        committed(assignment()).forEach { (partition, metadata) ->
            seek(partition, metadata.offset())
        }
    }

}
