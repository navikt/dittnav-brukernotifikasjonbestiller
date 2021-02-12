package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringEventService
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)

    val environment = Environment()
    val healthService = HealthService(this)

    var beskjedConsumer = initializeBeskjedProcessor()
    var oppgaveConsumer = initializeOppgaveProcessor()
    var statusoppdateringConsumer = initializeStatusoppdateringProcessor()
    var doneConsumer = initializeDoneProcessor()

    private fun initializeBeskjedProcessor(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedMainTopicName, KafkaProducer<Nokkel, Beskjed>(producerProps))
        val beskjedEventProcessor = BeskjedEventService(kafkaProducerWrapper)
        return KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(consumerProps, beskjedEventProcessor)
    }

    private fun initializeOppgaveProcessor(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveMainTopicName, KafkaProducer<Nokkel, Oppgave>(producerProps))
        val oppgaveEventProcessor = OppgaveEventService(kafkaProducerWrapper)
        return KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(consumerProps, oppgaveEventProcessor)
    }

    private fun initializeStatusoppdateringProcessor(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.statusoppdateringMainTopicName, KafkaProducer<Nokkel, Statusoppdatering>(producerProps))
        val statusoppdateringEventProcessor = StatusoppdateringEventService(kafkaProducerWrapper)
        return KafkaConsumerSetup.setupConsumerForTheStatusoppdateringInputTopic(consumerProps, statusoppdateringEventProcessor)
    }

    private fun initializeDoneProcessor(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneMainTopicName, KafkaProducer<Nokkel, Done>(producerProps))
        val doneEventProcessor = DoneEventService(kafkaProducerWrapper)
        return KafkaConsumerSetup.setupConsumerForTheDoneInputTopic(consumerProps, doneEventProcessor)
    }


    fun reinitializeConsumers() {
        if (beskjedConsumer.isCompleted()) {
            beskjedConsumer = initializeBeskjedProcessor()
            log.info("beskjedConsumer har blitt reinstansiert.")
        } else {
            log.warn("beskjedConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (oppgaveConsumer.isCompleted()) {
            oppgaveConsumer = initializeOppgaveProcessor()
            log.info("oppgaveConsumer har blitt reinstansiert.")
        } else {
            log.warn("oppgaveConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (statusoppdateringConsumer.isCompleted()) {
            statusoppdateringConsumer = initializeStatusoppdateringProcessor()
            log.info("statusoppdateringConsumer har blitt reinstansiert.")
        } else {
            log.warn("statusoppdateringConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (doneConsumer.isCompleted()) {
            doneConsumer = initializeDoneProcessor()
            log.info("doneConsumer har blitt reinstansiert.")
        } else {
            log.warn("doneConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }
    }
}
