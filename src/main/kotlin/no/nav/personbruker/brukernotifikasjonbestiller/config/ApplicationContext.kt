package no.nav.personbruker.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.brukernotifikasjonbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.brukernotifikasjonbestiller.done.DoneEventService
import no.nav.personbruker.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.brukernotifikasjonbestiller.oppgave.OppgaveEventService
import no.nav.personbruker.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringEventService
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper
import org.apache.kafka.clients.producer.KafkaProducer

class ApplicationContext {

    val environment = Environment()
    val healthService = HealthService(this)

    val beskjedConsumer = initializeBeskjedConsumer()
    val oppgaveConsumer = initializeOppgaveConsumer()
    val statusoppdateringConsumer = initializeStatusoppdateringConsumer()
    val doneConsumer = initializeDoneConsumer()

    val beskjedProducer = initializeBeskjedProducer()
    val oppgaveProducer = initializeOppgaveProducer()
    val statusoppdateringProducer = initializeStatusoppdateringProducer()
    val doneProducer = initializeDoneProducer()

    private fun initializeBeskjedConsumer(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val beskjedEventProcessor = BeskjedEventService()
        return KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(consumerProps, beskjedEventProcessor)
    }

    private fun initializeBeskjedProducer(): Producer<Nokkel, Beskjed> {
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducer = KafkaProducer<Nokkel, Beskjed>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedMainTopicName, kafkaProducer)
        return Producer(kafkaProducerWrapper)
    }

    private fun initializeOppgaveConsumer(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val oppgaveEventProcessor = OppgaveEventService()
        return KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(consumerProps, oppgaveEventProcessor)
    }

    private fun initializeOppgaveProducer(): Producer<Nokkel, Oppgave> {
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducer = KafkaProducer<Nokkel, Oppgave>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveMainTopicName, kafkaProducer)
        return Producer(kafkaProducerWrapper)
    }

    private fun initializeStatusoppdateringConsumer(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val statusoppdateringEventProcessor = StatusoppdateringEventService()
        return KafkaConsumerSetup.setupConsumerForTheStatusoppdateringInputTopic(consumerProps, statusoppdateringEventProcessor)
    }

    private fun initializeStatusoppdateringProducer(): Producer<Nokkel, Statusoppdatering> {
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducer = KafkaProducer<Nokkel, Statusoppdatering>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.statusoppdateringMainTopicName, kafkaProducer)
        return Producer(kafkaProducerWrapper)
    }

    private fun initializeDoneConsumer(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val doneEventProcessor = DoneEventService()
        return KafkaConsumerSetup.setupConsumerForTheDoneInputTopic(consumerProps, doneEventProcessor)
    }

    private fun initializeDoneProducer(): Producer<Nokkel, Done> {
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducer = KafkaProducer<Nokkel, Done>(producerProps)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneMainTopicName, kafkaProducer)
        return Producer(kafkaProducerWrapper)
    }


}
