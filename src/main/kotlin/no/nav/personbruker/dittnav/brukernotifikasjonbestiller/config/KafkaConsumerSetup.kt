package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

object KafkaConsumerSetup {

    private val log = KotlinLogging.logger {  }

    fun startAllKafkaPollers(appContext: ApplicationContext) {
        checkInputTopics(appContext)
    }

    private fun checkInputTopics(appContext: ApplicationContext) {
        if(shouldPollBeskjedInput()) {
            appContext.beskjedInputConsumer.startPolling()
        } else {
            log.info { "Unnlater å starte polling av beskjed-input" }
        }

        if(shouldPollOppgaveInput()) {
            appContext.oppgaveInputConsumer.startPolling()
        } else {
            log.info { "Unnlater å starte polling av oppgave-input" }
        }

        if(shouldPollInnboksInput()) {
            appContext.innboksInputConsumer.startPolling()
        } else {
            log.info { "Unnlater å starte polling av innboks-input" }
        }

        if(shouldPollDoneInput()) {
            appContext.doneInputConsumer.startPolling()
        } else {
            log.info { "Unnlater å starte polling av done-input" }
        }
    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info { "Begynner å stoppe kafka-pollerne..." }

        stopPollingIfNotCompleted(
            appContext.beskjedInputConsumer,
            appContext.oppgaveInputConsumer,
            appContext.innboksInputConsumer,
            appContext.doneInputConsumer
        )

        log.info { "...ferdig med å stoppe kafka-pollerne." }
    }

    private suspend fun stopPollingIfNotCompleted(vararg consumers: Consumer<*, *>) {
        consumers.forEach { consumer ->
            if (!consumer.isCompleted()) {
                consumer.stopPolling()
            }
        }
    }

    fun <T> setUpConsumerForInputTopic(topicName: String, kafkaProps: Properties, eventProcessor: EventBatchProcessorService<NokkelInput, T>): Consumer<NokkelInput, T> {
        val kafkaConsumer = KafkaConsumer<NokkelInput, T>(kafkaProps)
        return Consumer(topicName, kafkaConsumer, eventProcessor)
    }

    suspend fun restartPolling(appContext: ApplicationContext) {
        stopAllKafkaConsumers(appContext)
        appContext.reinitializeConsumers()
        startAllKafkaPollers(appContext)
    }
}
