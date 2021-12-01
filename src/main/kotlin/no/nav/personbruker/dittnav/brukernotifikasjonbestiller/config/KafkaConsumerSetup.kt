package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object KafkaConsumerSetup {

    private val log: Logger = LoggerFactory.getLogger(KafkaConsumerSetup::class.java)

    fun startAllKafkaPollers(appContext: ApplicationContext) {

        checkLegacyTopics(appContext)
        checkInputTopics(appContext)
    }

    private fun checkLegacyTopics(appContext: ApplicationContext) {
        if(shouldPollBeskjedLegacy()) {
            appContext.beskjedLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av beskjed-legacy")
        }

        if(shouldPollOppgaveLegacy()) {
            appContext.oppgaveLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av oppgave-legacy")
        }

        if(shouldPollInnboksLegacy()) {
            appContext.innboksLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av innboks-legacy")
        }

        if(shouldPollStatusoppdateringLegacy()) {
            appContext.statusoppdateringLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av statusoppdatering-legacy")
        }

        if(shouldPollDoneLegacy()) {
            appContext.doneLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av done-legacy")
        }
    }

    fun checkInputTopics(appContext: ApplicationContext) {
        if(shouldPollBeskjedInput()) {
            appContext.beskjedInputConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av beskjed-input")
        }

        if(shouldPollOppgaveInput()) {
            appContext.oppgaveInputConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av oppgave-input")
        }

        if(shouldPollInnboksInput()) {
            appContext.innboksInputConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av innboks-input")
        }

        if(shouldPollStatusoppdateringInput()) {
            appContext.statusoppdateringInputConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av statusoppdatering-input")
        }

        if(shouldPollDoneInput()) {
            appContext.doneInputConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av done-input")
        }
    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info("Begynner å stoppe kafka-pollerne...")

        stopPollingIfNotCompleted(
            appContext.beskjedLegacyConsumer,
            appContext.beskjedInputConsumer,
            appContext.oppgaveLegacyConsumer,
            appContext.oppgaveInputConsumer,
            appContext.innboksLegacyConsumer,
            appContext.innboksInputConsumer,
            appContext.statusoppdateringLegacyConsumer,
            appContext.statusoppdateringInputConsumer,
            appContext.doneLegacyConsumer,
            appContext.doneInputConsumer
        )

        log.info("...ferdig med å stoppe kafka-pollerne.")
    }

    private suspend fun stopPollingIfNotCompleted(vararg consumers: Consumer<*, *>) {
        consumers.forEach { consumer ->
            if (!consumer.isCompleted()) {
                consumer.stopPolling()
            }
        }
    }

    fun <T> setUpConsumerForLegacyTopic(topicName: String, kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, T>): Consumer<Nokkel, T> {
        val kafkaConsumer = KafkaConsumer<Nokkel, T>(kafkaProps)
        return Consumer(topicName, kafkaConsumer, eventProcessor)
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
