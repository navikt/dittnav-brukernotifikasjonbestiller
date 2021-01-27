package no.nav.personbruker.brukernotifikasjonbestiller.config

import no.nav.personbruker.brukernotifikasjonbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.brukernotifikasjonbestiller.done.DoneEventService
import no.nav.personbruker.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.brukernotifikasjonbestiller.oppgave.OppgaveEventService

class ApplicationContext {

    val environment = Environment()
    val healthService = HealthService(this)

    private val beskjedKafkaConsumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
    private val beskjedEventProcessor = BeskjedEventService()
    val beskjedConsumer = initializeBeskjedConsumer()

    private val oppgaveKafkaConsumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
    private val oppgaveEventProcessor = OppgaveEventService()
    val oppgaveConsumer = initializeOppgaveConsumer()

    private val doneKafkaConsumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
    private val doneEventProcessor = DoneEventService()
    val doneConsumer = initializeDoneConsumer()

    private fun initializeBeskjedConsumer() = KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(beskjedKafkaConsumerProps, beskjedEventProcessor)
    private fun initializeOppgaveConsumer() = KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(oppgaveKafkaConsumerProps, oppgaveEventProcessor)
    private fun initializeDoneConsumer() = KafkaConsumerSetup.setupConsumerForTheDoneInputTopic(doneKafkaConsumerProps, doneEventProcessor)
}
