package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.legacy.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateDoneEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling.PeriodicConsumerPollingCheck
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingParser
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.HealthService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveLegacyTransformer
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
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)
    private val serviceUserMapper = initializeServiceUserMapper(environment.serviceUserMapping)

    var internBeskjedKafkaProducer = initializeInternBeskjedProducer()
    var internOppgaveKafkaProducer = initializeInternOppgaveProducer()
    var internInnboksKafkaProducer = initializeInternInnboksProducer()
    var internDoneKafkaProducer = initializeInternDoneProducer()
    var internStatusoppdateringKafkaProducer = initializeInternStatusoppdateringProducer()

    var beskjedLegacyConsumer = initializeBeskjedLegacyProcessor()
    var oppgaveLegacyConsumer = initializeOppgaveLegacyProcessor()
    var innboksLegacyConsumer = initializeInnboksLegacyProcessor()
    var statusoppdateringLegacyConsumer = initializeStatusoppdateringLegacyProcessor()
    var doneLegacyConsumer = initializeDoneLegacyProcessor()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()

    private fun initializeBeskjedLegacyProcessor(): Consumer<NokkelLegacy, BeskjedLegacy> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducer, feilresponsKafkaProducer)
        val beskjedTransformer = BeskjedLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val beskjedEventService = BeskjedLegacyEventService(beskjedTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, beskjedEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.beskjedLegacyTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveLegacyProcessor(): Consumer<NokkelLegacy, OppgaveLegacy> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducer, feilresponsKafkaProducer)
        val oppgaveTransformer = OppgaveLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val oppgaveEventService = OppgaveLegacyEventService(oppgaveTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.oppgaveLegacyTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeInnboksLegacyProcessor(): Consumer<NokkelLegacy, InnboksLegacy> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.INNBOKS)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.INNBOKS)
        val innboksEventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internInnboksKafkaProducer, feilresponsKafkaProducer)
        val innboksTransformer = InnboksLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val innboksEventService = InnboksLegacyEventService(innboksTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, innboksEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.innboksLegacyTopicName, consumerProps, innboksEventService)
    }

    private fun initializeStatusoppdateringLegacyProcessor(): Consumer<NokkelLegacy, StatusoppdateringLegacy> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val handleDuplicateEvents = HandleDuplicateEvents(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.STATUSOPPDATERING)
        val statusoppdateringEventDispatcher = EventDispatcher(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository, internStatusoppdateringKafkaProducer, feilresponsKafkaProducer)
        val statusoppdateringTransformer = StatusoppdateringLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val statusoppdateringEventService = StatusoppdateringLegacyEventService(statusoppdateringTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, statusoppdateringEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.statusoppdateringLegacyTopicName, consumerProps, statusoppdateringEventService)
    }

    private fun initializeDoneLegacyProcessor(): Consumer<NokkelLegacy, DoneLegacy> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val handleDuplicateDoneEvents = HandleDuplicateDoneEvents(Eventtype.DONE, brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.DONE)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internDoneKafkaProducer, feilresponsKafkaProducer)
        val doneTransformer = DoneLegacyTransformer(serviceUserMapper)
        val feilresponsTransformer = FeilresponsLegacyTransformer(serviceUserMapper)
        val doneEventService = DoneLegacyEventService(doneTransformer, feilresponsTransformer, metricsCollector, handleDuplicateDoneEvents, doneEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForLegacyTopic(environment.doneLegacyTopicName, consumerProps, doneEventService)
    }

    private fun initializeInternBeskjedProducer(): Producer<NokkelIntern, BeskjedIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.BESKJEDINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.beskjedInternTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternOppgaveProducer(): Producer<NokkelIntern, OppgaveIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.OPPGAVEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.oppgaveInternTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternInnboksProducer(): Producer<NokkelIntern, InnboksIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.INNBOKSINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, InnboksIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.innboksInternTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternDoneProducer(): Producer<NokkelIntern, DoneIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.DONEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.doneInternTopicName, kafkaProducer)
        return producer
    }

    private fun initializeInternStatusoppdateringProducer(): Producer<NokkelIntern, StatusoppdateringIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.STATUSOPPDATERINGINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, StatusoppdateringIntern>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.statusoppdateringInternTopicName, kafkaProducer)
        return producer
    }

    private fun initializeFeilresponsProducer(eventtype: Eventtype): Producer<NokkelFeilrespons, Feilrespons> {
        val producerProps = Kafka.producerFeilresponsProps(environment, eventtype)
        val kafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(producerProps)
        kafkaProducer.initTransactions()
        val producer = Producer(environment.feilresponsTopicName, kafkaProducer)
        return producer
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
