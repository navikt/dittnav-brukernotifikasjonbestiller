package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.brukernotifikasjon.schemas.input.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling.PeriodicConsumerPollingCheck
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingParser
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.TopicSource.AIVEN
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.TopicSource.ON_PREM
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyTransformer
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

    private val httpClient = HttpClientBuilder.build()
    private val nameResolver = ProducerNameResolver(httpClient, environment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsReporter = resolveMetricsReporter(environment)
    private val metricsCollectorLegacy = MetricsCollectorLegacy(metricsReporter, nameScrubber)
    private val metricsCollector = MetricsCollector(metricsReporter)
    private val serviceUserMapper = initializeServiceUserMapper(environment.serviceUserMapping)

    var internBeskjedKafkaProducerLegacy = initializeInternBeskjedProducer(ON_PREM)
    var internOppgaveKafkaProducerLegacy = initializeInternOppgaveProducer(ON_PREM)
    var internInnboksKafkaProducerLegacy = initializeInternInnboksProducer(ON_PREM)
    var internDoneKafkaProducerLegacy = initializeInternDoneProducer(ON_PREM)
    var internStatusoppdateringKafkaProducerLegacy = initializeInternStatusoppdateringProducer(ON_PREM)

    var internBeskjedKafkaProducer = initializeInternBeskjedProducer(AIVEN)
    var internOppgaveKafkaProducer = initializeInternOppgaveProducer(AIVEN)
    var internInnboksKafkaProducer = initializeInternInnboksProducer(AIVEN)
    var internDoneKafkaProducer = initializeInternDoneProducer(AIVEN)
    var internStatusoppdateringKafkaProducer = initializeInternStatusoppdateringProducer(AIVEN)

    var beskjedLegacyConsumer = initializeBeskjedLegacyProcessor()
    var oppgaveLegacyConsumer = initializeOppgaveLegacyProcessor()
    var innboksLegacyConsumer = initializeInnboksLegacyProcessor()
    var statusoppdateringLegacyConsumer = initializeStatusoppdateringLegacyProcessor()
    var doneLegacyConsumer = initializeDoneLegacyProcessor()

    var beskjedInputConsumer = initializeBeskjedInputProcessor()
    var oppgaveInputConsumer = initializeOppgaveInputProcessor()
    var innboksInputConsumer = initializeInnboksInputProcessor()
    var statusoppdateringInputConsumer = initializeStatusoppdateringInputProcessor()
    var doneInputConsumer = initializeDoneInputProcessor()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()

    private fun initializeBeskjedLegacyProcessor(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerPropsLegacy(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED, ON_PREM)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducerLegacy, feilresponsKafkaProducer)
        val beskjedTransformer = BeskjedLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val beskjedEventService = BeskjedLegacyEventService(beskjedTransformer, feilresponsTransformer, metricsCollectorLegacy, handleDuplicateEvents, beskjedEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.beskjedLegacyTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeBeskjedInputProcessor(): Consumer<NokkelInput, BeskjedInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED, AIVEN)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducer, feilresponsKafkaProducer)
        val beskjedEventService = BeskjedInputEventService(metricsCollector, handleDuplicateEvents, beskjedEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.beskjedInputTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveLegacyProcessor(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerPropsLegacy(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE, ON_PREM)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducerLegacy, feilresponsKafkaProducer)
        val oppgaveTransformer = OppgaveLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val oppgaveEventService = OppgaveLegacyEventService(oppgaveTransformer, feilresponsTransformer, metricsCollectorLegacy, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.oppgaveLegacyTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeOppgaveInputProcessor(): Consumer<NokkelInput, OppgaveInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE, AIVEN)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducer, feilresponsKafkaProducer)
        val oppgaveEventService = OppgaveInputEventService(metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.oppgaveInputTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeInnboksLegacyProcessor(): Consumer<Nokkel, Innboks> {
        val consumerProps = Kafka.consumerPropsLegacy(environment, Eventtype.INNBOKS)
        val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.INNBOKS, ON_PREM)
        val innboksEventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internInnboksKafkaProducerLegacy, feilresponsKafkaProducer)
        val innboksTransformer = InnboksLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val innboksEventService = InnboksLegacyEventService(innboksTransformer, feilresponsTransformer, metricsCollectorLegacy, handleDuplicateEvents, innboksEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.innboksLegacyTopicName, consumerProps, innboksEventService)
    }

    private fun initializeInnboksInputProcessor(): Consumer<NokkelInput, InnboksInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.INNBOKS)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.INNBOKS, AIVEN)
        val innboksEventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internInnboksKafkaProducer, feilresponsKafkaProducer)
        val innboksEventService = InnboksInputEventService(metricsCollector, handleDuplicateEvents, innboksEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.innboksInputTopicName, consumerProps, innboksEventService)
    }

    private fun initializeStatusoppdateringLegacyProcessor(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerPropsLegacy(environment, Eventtype.STATUSOPPDATERING)
        val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.STATUSOPPDATERING, ON_PREM)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internStatusoppdateringKafkaProducerLegacy, feilresponsKafkaProducer)
        val statusoppdateringTransformer = StatusoppdateringLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(statusoppdateringTransformer, feilresponsTransformer, metricsCollectorLegacy, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.statusoppdateringLegacyTopicName, consumerProps, statusoppdateringEventService)
    }

    private fun initializeStatusoppdateringInputProcessor(): Consumer<NokkelInput, StatusoppdateringInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.STATUSOPPDATERING, AIVEN)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internStatusoppdateringKafkaProducer, feilresponsKafkaProducer)
        val statusoppdateringEventService = StatusoppdateringInputEventService(metricsCollector, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.statusoppdateringInputTopicName, consumerProps, statusoppdateringEventService)
    }

    private fun initializeDoneLegacyProcessor(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerPropsLegacy(environment, Eventtype.DONE)
        val handleDuplicateDoneEvents = HandleDuplicateDoneEventsLegacy(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.DONE, ON_PREM)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internDoneKafkaProducerLegacy, feilresponsKafkaProducer)
        val doneTransformer = DoneLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val doneEventService = DoneLegacyEventService(doneTransformer, feilresponsTransformer, metricsCollectorLegacy, handleDuplicateDoneEvents, doneEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.doneLegacyTopicName, consumerProps, doneEventService)
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

    private fun initializeServiceUserMapper(mappingStrings: List<String>): ServiceUserMapper {
        val mappings = ServiceUserMappingParser.parseMappingStrings(mappingStrings)

        return ServiceUserMapper(mappings)
    }

    fun reinitializeConsumers() {
        if (beskjedLegacyConsumer.isCompleted()) {
            beskjedLegacyConsumer = initializeBeskjedLegacyProcessor()
            log.info("beskjedLegacyConsumer har blitt reinstansiert.")
        } else {
            log.warn("beskjedLegacyConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (oppgaveLegacyConsumer.isCompleted()) {
            oppgaveLegacyConsumer = initializeOppgaveLegacyProcessor()
            log.info("oppgaveLegacyConsumer har blitt reinstansiert.")
        } else {
            log.warn("oppgaveLegacyConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (innboksLegacyConsumer.isCompleted()) {
            innboksLegacyConsumer = initializeInnboksLegacyProcessor()
            log.info("innboksLegacyConsumer har blitt reinstansiert.")
        } else {
            log.warn("innboksLegacyConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (statusoppdateringLegacyConsumer.isCompleted()) {
            statusoppdateringLegacyConsumer = initializeStatusoppdateringLegacyProcessor()
            log.info("statusoppdateringLegacyConsumer har blitt reinstansiert.")
        } else {
            log.warn("statusoppdateringLegacyConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
        }

        if (doneLegacyConsumer.isCompleted()) {
            doneLegacyConsumer = initializeDoneLegacyProcessor()
            log.info("doneLegacyConsumer har blitt reinstansiert.")
        } else {
            log.warn("doneLegacyConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv.")
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
