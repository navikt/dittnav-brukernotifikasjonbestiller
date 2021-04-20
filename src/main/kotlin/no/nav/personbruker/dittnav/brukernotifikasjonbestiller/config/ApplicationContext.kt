package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaProducerWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringEventService
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influx.InfluxMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influx.SensuConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)

    val environment = Environment()
    val healthService = HealthService(this)
    val database: Database = PostgresDatabase(environment)
    val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)

    private val httpClient = HttpClientBuilder.build()
    private val nameResolver = ProducerNameResolver(httpClient, environment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsReporter = resolveMetricsReporter(environment)
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

    var beskjedConsumer = initializeBeskjedProcessor()
    var oppgaveConsumer = initializeOppgaveProcessor()
    var statusoppdateringConsumer = initializeStatusoppdateringProcessor()
    var doneConsumer = initializeDoneProcessor()

    private fun initializeBeskjedProcessor(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)

        val producerProps = Kafka.producerProps(environment, Eventtype.BESKJEDINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps)
        kafkaProducer.initTransactions()
        val internalKafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedHovedTopicName, kafkaProducer)

        val feilresponsProducerProps = Kafka.producerProps(environment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducerWrapper = KafkaProducerWrapper(Kafka.feilresponsTopicName, KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps))

        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val beskjedEventProcessor = BeskjedEventService(internalKafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector, handleDuplicateEvents, beskjedEventDispatcher)

        return KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(consumerProps, beskjedEventProcessor)
    }

    private fun initializeOppgaveProcessor(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val producerProps = Kafka.producerProps(environment, Eventtype.OPPGAVE)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveHovedTopicName, KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps))
        val feilresponsProducerProps = Kafka.producerProps(environment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducerWrapper = KafkaProducerWrapper(Kafka.feilresponsTopicName, KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps))
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository)
        val oppgaveEventProcessor = OppgaveEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(consumerProps, oppgaveEventProcessor)
    }

    private fun initializeStatusoppdateringProcessor(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val producerProps = Kafka.producerProps(environment, Eventtype.STATUSOPPDATERING)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.statusoppdateringHovedTopicName, KafkaProducer<NokkelIntern, StatusoppdateringIntern>(producerProps))
        val feilresponsProducerProps = Kafka.producerProps(environment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducerWrapper = KafkaProducerWrapper(Kafka.feilresponsTopicName, KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps))
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val statusoppdateringEventProcessor = StatusoppdateringEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheStatusoppdateringInputTopic(consumerProps, statusoppdateringEventProcessor)
    }

    private fun initializeDoneProcessor(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val producerProps = Kafka.producerProps(environment, Eventtype.DONE)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneHovedTopicName, KafkaProducer<NokkelIntern, DoneIntern>(producerProps))
        val feilresponsProducerProps = Kafka.producerProps(environment, Eventtype.FEILRESPONS)
        val feilresponsKafkaProducerWrapper = KafkaProducerWrapper(Kafka.feilresponsTopicName, KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps))
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val doneEventProcessor = DoneEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector, handleDuplicateEvents, doneEventDispatcher)
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

    private fun resolveMetricsReporter(environment: Environment): MetricsReporter {
        return if (environment.sensuHost == "" || environment.sensuHost == "stub") {
            StubMetricsReporter()
        } else {
            val sensuConfig = SensuConfig(
                    applicationName = environment.applicationName,
                    hostName = environment.sensuHost,
                    hostPort = environment.sensuPort,
                    clusterName = environment.clusterName,
                    namespace = environment.namespace,
                    eventsTopLevelName = "dittnav-brukernotifikasjonbestiller",
                    enableEventBatching = environment.sensuBatchingEnabled,
                    eventBatchesPerSecond = environment.sensuBatchesPerSecond
            )

            InfluxMetricsReporter(sensuConfig)
        }
    }
}
