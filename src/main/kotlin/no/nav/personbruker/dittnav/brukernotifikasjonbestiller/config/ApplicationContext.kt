package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.input.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling.PeriodicConsumerPollingCheck
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.TopicSource.AIVEN
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringInputEventService
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxMetricsReporter
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)

    val environment = Environment()
    val healthService = HealthService(this)
    val database: Database = PostgresDatabase(environment)
    val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)

    private val metricsReporter = resolveMetricsReporter(environment)
    private val metricsCollector = MetricsCollector(metricsReporter)

    var internBeskjedKafkaProducer = initializeInternBeskjedProducer(AIVEN)
    var internOppgaveKafkaProducer = initializeInternOppgaveProducer(AIVEN)
    var internInnboksKafkaProducer = initializeInternInnboksProducer(AIVEN)
    var internDoneKafkaProducer = initializeInternDoneProducer(AIVEN)
    var internStatusoppdateringKafkaProducer = initializeInternStatusoppdateringProducer(AIVEN)

    var beskjedInputConsumer = initializeBeskjedInputProcessor()
    var oppgaveInputConsumer = initializeOppgaveInputProcessor()
    var innboksInputConsumer = initializeInnboksInputProcessor()
    var statusoppdateringInputConsumer = initializeStatusoppdateringInputProcessor()
    var doneInputConsumer = initializeDoneInputProcessor()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()



    private fun initializeBeskjedInputProcessor(): Consumer<NokkelInput, BeskjedInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED, AIVEN)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducer, feilresponsKafkaProducer)
        val beskjedEventService = BeskjedInputEventService(metricsCollector, handleDuplicateEvents, beskjedEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.beskjedInputTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveInputProcessor(): Consumer<NokkelInput, OppgaveInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE, AIVEN)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducer, feilresponsKafkaProducer)
        val oppgaveEventService = OppgaveInputEventService(metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.oppgaveInputTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeInnboksInputProcessor(): Consumer<NokkelInput, InnboksInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.INNBOKS)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.INNBOKS, AIVEN)
        val innboksEventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internInnboksKafkaProducer, feilresponsKafkaProducer)
        val innboksEventService = InnboksInputEventService(metricsCollector, handleDuplicateEvents, innboksEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.innboksInputTopicName, consumerProps, innboksEventService)
    }

    private fun initializeStatusoppdateringInputProcessor(): Consumer<NokkelInput, StatusoppdateringInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.STATUSOPPDATERING, AIVEN)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internStatusoppdateringKafkaProducer, feilresponsKafkaProducer)
        val statusoppdateringEventService = StatusoppdateringInputEventService(metricsCollector, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.statusoppdateringInputTopicName, consumerProps, statusoppdateringEventService)
    }


    private fun initializeDoneInputProcessor(): Consumer<NokkelInput, DoneInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.DONE, AIVEN)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internDoneKafkaProducer, feilresponsKafkaProducer)
        val doneEventService = DoneInputEventService(metricsCollector, handleDuplicateEvents, doneEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.doneInputTopicName, consumerProps, doneEventService)
    }

    private fun initializeInternBeskjedProducer(eventSource: TopicSource): Producer<NokkelIntern, BeskjedIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.BESKJEDINTERN, eventSource)
        val kafkaProducer = KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.beskjedInternTopicName, kafkaProducer)
    }

    private fun initializeInternOppgaveProducer(eventSource: TopicSource): Producer<NokkelIntern, OppgaveIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.OPPGAVEINTERN, eventSource)
        val kafkaProducer = KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.oppgaveInternTopicName, kafkaProducer)
    }

    private fun initializeInternInnboksProducer(eventSource: TopicSource): Producer<NokkelIntern, InnboksIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.INNBOKSINTERN, eventSource)
        val kafkaProducer = KafkaProducer<NokkelIntern, InnboksIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.innboksInternTopicName, kafkaProducer)
    }

    private fun initializeInternDoneProducer(eventSource: TopicSource): Producer<NokkelIntern, DoneIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.DONEINTERN, eventSource)
        val kafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.doneInternTopicName, kafkaProducer)
    }

    private fun initializeInternStatusoppdateringProducer(eventSource: TopicSource): Producer<NokkelIntern, StatusoppdateringIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.STATUSOPPDATERINGINTERN, eventSource)
        val kafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.statusoppdateringInternTopicName, kafkaProducer)
    }

    private fun initializeFeilresponsProducer(eventtype: Eventtype, eventSource: TopicSource): Producer<NokkelFeilrespons, Feilrespons> {
        val producerProps = Kafka.producerFeilresponsProps(environment, eventtype, eventSource)
        val kafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.feilresponsTopicName, kafkaProducer)
    }

    private fun initializePeriodicConsumerPollingCheck(): PeriodicConsumerPollingCheck {
        return PeriodicConsumerPollingCheck(this)
    }

    fun reinitializeConsumers() {
        if (beskjedInputConsumer.isCompleted()) {
            beskjedInputConsumer = initializeBeskjedInputProcessor()
            log.info("beskjedInputConsumer har blitt reinstansiert.")
        } else {
            log.warn("beskjedInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (oppgaveInputConsumer.isCompleted()) {
            oppgaveInputConsumer = initializeOppgaveInputProcessor()
            log.info("oppgaveInputConsumer har blitt reinstansiert.")
        } else {
            log.warn("oppgaveInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (innboksInputConsumer.isCompleted()) {
            innboksInputConsumer = initializeInnboksInputProcessor()
            log.info("innboksInputConsumer har blitt reinstansiert.")
        } else {
            log.warn("innboksInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (statusoppdateringInputConsumer.isCompleted()) {
            statusoppdateringInputConsumer = initializeStatusoppdateringInputProcessor()
            log.info("statusoppdateringInputConsumer har blitt reinstansiert.")
        } else {
            log.warn("statusoppdateringInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (doneInputConsumer.isCompleted()) {
            doneInputConsumer = initializeDoneInputProcessor()
            log.info("doneInputConsumer har blitt reinstansiert.")
        } else {
            log.warn("doneInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
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
