package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionForwarder
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext

class InputTopicConsumer(
    private val topics: List<String>,
    private val kafkaConsumer: Consumer<NokkelInput, GenericRecord>,
    private val varselActionForwarder: VarselActionForwarder,
    private val job: Job = Job()
) : CoroutineScope {

    private val log = KotlinLogging.logger { }
    private val secureLog = KotlinLogging.logger("secureLog")

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    suspend fun stopPolling() {
        if (!job.isCompleted) {
            job.cancelAndJoin()
        }
    }

    fun isStopped(): Boolean {
        return !job.isActive
    }

    fun startPolling() = launch {
        kafkaConsumer.use { consumer ->
            consumer.subscribe(topics)

            while (job.isActive) {
                pollForAndRelayEvents()
            }
        }
    }

    private suspend fun pollForAndRelayEvents() = withContext(Dispatchers.IO) {
        try {
            kafkaConsumer.poll(Duration.of(100, ChronoUnit.MILLIS))
                .takeIf { it.count() > 0 }
                ?.filter { it.value() != null }
                ?.forEach { varselActionForwarder.forwardVarsel(it.key(), it.value()) }
                ?.let { kafkaConsumer.commitSync() }

        } catch (re: RetriableKafkaException) {
            log.warn { "Post mot Kafka feilet, prøver igjen senere." }
            secureLog.warn(re) { "Post mot Kafka feilet, prøver igjen senere." }
            rollbackOffset()

        } catch (ure: UnretriableKafkaException) {
            log.warn { "Alvorlig feil ved post mot kafka. Stopper polling." }
            secureLog.warn(ure) { "Alvorlig feil ved post mot kafka. Stopper polling." }
            stopPolling()

        } catch (re: RetriableException) {
            log.warn { "Polling mot Kafka feilet, prøver igjen senere." }
            secureLog.warn(re) { "Polling mot Kafka feilet, prøver igjen senere." }
            rollbackOffset()

        } catch (ce: CancellationException) {
            log.info { "Denne coroutine-en ble stoppet. ${ce.message}" }
            secureLog.warn(ce) { "Denne coroutine-en ble stoppet. ${ce.message}" }
        } catch (ie: InterruptedException) {
            log.warn { "Produsering av event til kafka ble avbrutt" }
            rollbackOffset()

        } catch (te: TimeoutException) {
            log.warn { "Produsering av event til kafka timet ut" }
            rollbackOffset()
        } catch (e: Exception) {
            log.error(e) { "Noe uventet feilet, stopper polling." }
            stopPolling()
        }
    }

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
