package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
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
            appContext.beskjedConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av beskjed")
        }

        if(shouldPollOppgave()) {
            appContext.oppgaveConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av oppgave")
        }

        if(shouldPollStatusoppdatering()) {
            appContext.statusoppdateringConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av statusoppdatering")
        }

        if(shouldPollDone()) {
            appContext.doneConsumer.startPolling()
        } else {
            log.info("Unnlater å starte polling av done")
        }

    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info("Begynner å stoppe kafka-pollerne...")
        if(!appContext.beskjedConsumer.isCompleted()) {
            appContext.beskjedConsumer.stopPolling()
        }

        if(!appContext.oppgaveConsumer.isCompleted()) {
            appContext.oppgaveConsumer.stopPolling()
        }

        if(!appContext.oppgaveConsumer.isCompleted()) {
            appContext.oppgaveConsumer.stopPolling()
        }

        if(!appContext.statusoppdateringConsumer.isCompleted()) {
            appContext.statusoppdateringConsumer.stopPolling()
        }

        if(!appContext.doneConsumer.isCompleted()) {
            appContext.doneConsumer.stopPolling()
        }
        log.info("...ferdig med å stoppe kafka-pollerne.")
    }

    fun setupConsumerForTheBeskjedInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Beskjed>): Consumer<Nokkel, Beskjed> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Beskjed>(kafkaProps)
        return Consumer(Kafka.beskjedAapenTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheOppgaveInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Oppgave>): Consumer<Nokkel, Oppgave> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Oppgave>(kafkaProps)
        return Consumer(Kafka.oppgaveAapenTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheStatusoppdateringInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Statusoppdatering>): Consumer<Nokkel, Statusoppdatering> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Statusoppdatering>(kafkaProps)
        return Consumer(Kafka.statusoppdateringAapenTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheDoneInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Done>): Consumer<Nokkel, Done> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Done>(kafkaProps)
        return Consumer(Kafka.doneAapenTopicName, kafkaConsumer, eventProcessor)
    }

    suspend fun restartPolling(appContext: ApplicationContext) {
        stopAllKafkaConsumers(appContext)
        appContext.reinitializeConsumers()
        startAllKafkaPollers(appContext)
    }
}
