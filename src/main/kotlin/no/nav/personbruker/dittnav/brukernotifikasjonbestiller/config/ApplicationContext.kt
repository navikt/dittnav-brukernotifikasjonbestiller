package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling.PeriodicConsumerPollingCheck
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringEventService
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influx.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influx.InfluxMetricsReporter
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

    var internBeskjedKafkaProducer = initializeInternBeskjedProducer()
    var internOppgaveKafkaProducer = initializeInternOppgaveProducer()
    var internDoneKafkaProducer = initializeInternDoneProducer()
    var internStatusoppdateringKafkaProducer = initializeInternStatusoppdateringProducer()

    var beskjedConsumer = initializeBeskjedProcessor()
    var oppgaveConsumer = initializeOppgaveProcessor()
    var statusoppdateringConsumer = initializeStatusoppdateringProcessor()
    var doneConsumer = initializeDoneProcessor()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()

    private fun initializeBeskjedProcessor(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducer, feilresponsKafkaProducer)
        val beskjedEventService = BeskjedEventService(metricsCollector, handleDuplicateEvents, beskjedEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveProcessor(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducer, feilresponsKafkaProducer)
        val oppgaveEventService = OppgaveEventService(metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(consumerProps, oppgaveEventService)
    }

    private fun initializeStatusoppdateringProcessor(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.STATUSOPPDATERING)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internStatusoppdateringKafkaProducer, feilresponsKafkaProducer)
        val statusoppdateringEventService = StatusoppdateringEventService(metricsCollector, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheStatusoppdateringInputTopic(consumerProps, statusoppdateringEventService)
    }

    private fun initializeDoneProcessor(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.DONE)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internDoneKafkaProducer, feilresponsKafkaProducer)
        val doneEventService = DoneEventService(metricsCollector, handleDuplicateEvents, doneEventDispatcher)
        return KafkaConsumerSetup.setupConsumerForTheDoneInputTopic(consumerProps, doneEventService)
    }

    private fun initializeInternBeskjedProducer(): Producer<NokkelIntern, BeskjedIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.BESKJEDINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(Kafka.beskjedHovedTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternOppgaveProducer(): Producer<NokkelIntern, OppgaveIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.OPPGAVEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(Kafka.oppgaveHovedTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternDoneProducer(): Producer<NokkelIntern, DoneIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.DONEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(Kafka.doneHovedTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternStatusoppdateringProducer(): Producer<NokkelIntern, StatusoppdateringIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.STATUSOPPDATERINGINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(Kafka.statusoppdateringHovedTopicName, kafkaProducer)
        return producer
    }

    private fun initializeFeilresponsProducer(eventtype: Eventtype): Producer<NokkelFeilrespons, Feilrespons> {
        val producerProps = Kafka.producerFeilresponsProps(environment, eventtype)
        val kafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(Kafka.feilresponsTopicName, kafkaProducer)
        return producer
    }

    private fun initializePeriodicConsumerPollingCheck(): PeriodicConsumerPollingCheck {
        return PeriodicConsumerPollingCheck(this)
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
        return if (environment.influxdbHost == "" || environment.influxdbHost == "stub") {
            StubMetricsReporter()
        } else {
            val sensuConfig = InfluxConfig(
                    applicationName = environment.applicationName,
                    hostName = environment.influxdbHost,
                    hostPort = environment.influxdbPort,
                    databaseName = environment.influxdbName,
                    retentionPolicyName = environment.influxdbRetentionPolicy,
                    clusterName = environment.clusterName,
                    namespace = environment.namespace,
                    userName = environment.influxdbUser,
                    password = environment.influxdbPassword
            )

            InfluxMetricsReporter(sensuConfig)
        }
    }

    fun reinitializePeriodicConsumerPollingCheck() {
        if (periodicConsumerPollingCheck.isCompleted()) {
            periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()
            log.info("periodicConsumerPollingCheck har blitt reinstansiert.")
        } else {
            log.warn("periodicConsumerPollingCheck kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }
    }
}
