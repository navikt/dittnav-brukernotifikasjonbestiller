package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling

import kotlinx.coroutines.*
import kotlinx.coroutines.time.delay
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.ApplicationContext
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.KafkaConsumerSetup
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthStatus
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class PeriodicConsumerPollingCheck(
        private val appContext: ApplicationContext,
        private val job: Job = Job()) : CoroutineScope {

    private val log: Logger = LoggerFactory.getLogger(PeriodicConsumerPollingCheck::class.java)
    private val minutesToWait = Duration.ofMinutes(30)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun start() {
        log.info("Periodisk sjekking av at konsumerne kjører har blitt aktivert, første sjekk skjer om $minutesToWait minutter.")
        launch {
            while (job.isActive) {
                delay(minutesToWait)
                checkIfConsumersAreRunningAndRestartIfNot()
            }
        }
    }

    suspend fun checkIfConsumersAreRunningAndRestartIfNot() {
        val stoppedConsumers = getConsumersThatHaveStopped()
        if (stoppedConsumers.isNotEmpty()) {
            restartPolling(stoppedConsumers)
        }
    }

    fun getConsumersThatHaveStopped(): MutableList<Eventtype> {
        val stoppedConsumers = mutableListOf<Eventtype>()

        if (appContext.beskjedConsumer.isStopped()) {
            stoppedConsumers.add(Eventtype.BESKJED)
        }
        if (appContext.oppgaveConsumer.isStopped()) {
            stoppedConsumers.add(Eventtype.OPPGAVE)
        }
        if (appContext.statusoppdateringConsumer.isStopped()) {
            stoppedConsumers.add(Eventtype.STATUSOPPDATERING)
        }
        if (appContext.doneConsumer.isStopped()) {
            stoppedConsumers.add(Eventtype.DONE)
        }
        return stoppedConsumers
    }

    suspend fun restartPolling(stoppedConsumers: MutableList<Eventtype>) {
        log.warn("Følgende konsumere hadde stoppet ${stoppedConsumers}, de(n) vil bli restartet.")
        KafkaConsumerSetup.restartPolling(appContext)
        log.info("$stoppedConsumers konsumern(e) har blitt restartet.")
    }

    suspend fun stop() {
        log.info("Stopper periodisk sjekking av at konsumerne kjører.")
        job.cancelAndJoin()
    }

    fun status(): HealthStatus {
        return when (job.isActive) {
            true -> HealthStatus("PeriodicConsumerPollingCheck", Status.OK, "Checker is running", false)
            false -> HealthStatus("PeriodicConsumerPollingCheck", Status.ERROR, "Checker is not running", false)
        }
    }

}
