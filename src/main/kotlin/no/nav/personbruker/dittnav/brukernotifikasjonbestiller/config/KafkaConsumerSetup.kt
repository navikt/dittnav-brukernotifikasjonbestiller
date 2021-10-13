package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object KafkaConsumerSetup {

    private val log: Logger = LoggerFactory.getLogger(KafkaConsumerSetup::class.java)

    fun startAllKafkaPollers(appContext: ApplicationContext) {
        if(shouldPollBeskjed()) {
            appContext.beskjedLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av beskjed")
        }

        if(shouldPollOppgave()) {
            appContext.oppgaveLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av oppgave")
        }

        if(shouldPollInnboks()) {
            appContext.innboksLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av innboks")
        }

        if(shouldPollStatusoppdatering()) {
            appContext.statusoppdateringLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av statusoppdatering")
        }

        if(shouldPollDone()) {
            appContext.doneLegacyConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av done")
        }
    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info("Begynner å stoppe kafka-pollerne...")
        if(!appContext.beskjedLegacyConsumer.isCompleted()) {
            appContext.beskjedLegacyConsumer.stopPolling()
        }

        if(!appContext.oppgaveLegacyConsumer.isCompleted()) {
            appContext.oppgaveLegacyConsumer.stopPolling()
        }

        if(!appContext.innboksLegacyConsumer.isCompleted()) {
            appContext.innboksLegacyConsumer.stopPolling()
        }

        if(!appContext.statusoppdateringLegacyConsumer.isCompleted()) {
            appContext.statusoppdateringLegacyConsumer.stopPolling()
        }

        if(!appContext.doneLegacyConsumer.isCompleted()) {
            appContext.doneLegacyConsumer.stopPolling()
        }
        log.info("...ferdig med å stoppe kafka-pollerne.")
    }

    fun <T> setUpConsumerForLegacyTopic(topicName: String, kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, T>): Consumer<Nokkel, T> {
        val kafkaConsumer = KafkaConsumer<Nokkel, T>(kafkaProps)
        return Consumer(topicName, kafkaConsumer, eventProcessor)
    }

    suspend fun restartPolling(appContext: ApplicationContext) {
        stopAllKafkaConsumers(appContext)
        appContext.reinitializeConsumers()
        startAllKafkaPollers(appContext)
    }
}
