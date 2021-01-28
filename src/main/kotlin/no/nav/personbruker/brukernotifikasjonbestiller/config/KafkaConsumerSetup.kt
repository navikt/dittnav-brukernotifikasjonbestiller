package no.nav.personbruker.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object KafkaConsumerSetup {

    private val log: Logger = LoggerFactory.getLogger(KafkaConsumerSetup::class.java)

    fun startAllKafkaPollers(appContext: ApplicationContext) {
        appContext.beskjedConsumer.startPolling()
        appContext.oppgaveConsumer.startPolling()
        appContext.statusoppdateringConsumer.startPolling()
        appContext.doneConsumer.startPolling()
    }

    suspend fun stopAllKafkaConsumers(appContext: ApplicationContext) {
        log.info("Begynner å stoppe kafka-pollerne...")
        appContext.beskjedConsumer.stopPolling()
        appContext.oppgaveConsumer.stopPolling()
        appContext.statusoppdateringConsumer.stopPolling()
        appContext.doneConsumer.stopPolling()
        log.info("...ferdig med å stoppe kafka-pollerne.")
    }

    fun setupConsumerForTheBeskjedInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Beskjed>): Consumer<Nokkel, Beskjed> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Beskjed>(kafkaProps)
        return Consumer(Kafka.beskjedInputTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheOppgaveInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Oppgave>): Consumer<Nokkel, Oppgave> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Oppgave>(kafkaProps)
        return Consumer(Kafka.oppgaveInputTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheStatusoppdateringInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Statusoppdatering>): Consumer<Nokkel, Statusoppdatering> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Statusoppdatering>(kafkaProps)
        return Consumer(Kafka.statusoppdateringInputTopicName, kafkaConsumer, eventProcessor)
    }

    fun setupConsumerForTheDoneInputTopic(kafkaProps: Properties, eventProcessor: EventBatchProcessorService<Nokkel, Done>): Consumer<Nokkel, Done> {
        val kafkaConsumer = KafkaConsumer<Nokkel, Done>(kafkaProps)
        return Consumer(Kafka.doneInputTopicName, kafkaConsumer, eventProcessor)
    }
}
