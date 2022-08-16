package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.netty.util.NetUtil
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.DoneInput
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedRapidProducer
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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveInputEventService
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxConfig
import no.nav.personbruker.dittnav.common.metrics.influxdb.InfluxMetricsReporter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.Properties

class ApplicationContext {

    private val log = LoggerFactory.getLogger(ApplicationContext::class.java)

    val environment = Environment()
    val healthService = HealthService(this)
    val database: Database = PostgresDatabase(environment)
    val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)

    private val metricsReporter = resolveMetricsReporter(environment)
    private val metricsCollector = MetricsCollector(metricsReporter)

    var internBeskjedKafkaProducer = initializeInternBeskjedProducer()
    var internOppgaveKafkaProducer = initializeInternOppgaveProducer()
    var internInnboksKafkaProducer = initializeInternInnboksProducer()
    var internDoneKafkaProducer = initializeInternDoneProducer()

    var beskjedInputConsumer = initializeBeskjedInputProcessor()
    var oppgaveInputConsumer = initializeOppgaveInputProcessor()
    var innboksInputConsumer = initializeInnboksInputProcessor()
    var doneInputConsumer = initializeDoneInputProcessor()

    val beskjedRapidProducer = initializeBeskjedRapidProducer()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()


    private fun initializeBeskjedInputProcessor(): Consumer<NokkelInput, BeskjedInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.BESKJED)
        val beskjedEventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internBeskjedKafkaProducer, feilresponsKafkaProducer)
        val beskjedEventService = BeskjedInputEventService(
            metricsCollector,
            handleDuplicateEvents,
            beskjedEventDispatcher,
            beskjedRapidProducer,
            environment.produceToRapid
        )
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.beskjedInputTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveInputProcessor(): Consumer<NokkelInput, OppgaveInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.OPPGAVE)
        val oppgaveEventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internOppgaveKafkaProducer, feilresponsKafkaProducer)
        val oppgaveEventService = OppgaveInputEventService(metricsCollector, handleDuplicateEvents, oppgaveEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.oppgaveInputTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeInnboksInputProcessor(): Consumer<NokkelInput, InnboksInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.INNBOKS)
        val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.INNBOKS)
        val innboksEventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internInnboksKafkaProducer, feilresponsKafkaProducer)
        val innboksEventService = InnboksInputEventService(metricsCollector, handleDuplicateEvents, innboksEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.innboksInputTopicName, consumerProps, innboksEventService)
    }

    private fun initializeDoneInputProcessor(): Consumer<NokkelInput, DoneInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val handleDuplicateEvents = HandleDuplicateDoneEvents(brukernotifikasjonbestillingRepository)
        val feilresponsKafkaProducer = initializeFeilresponsProducer(Eventtype.DONE)
        val doneEventDispatcher = EventDispatcher(Eventtype.DONE, brukernotifikasjonbestillingRepository, internDoneKafkaProducer, feilresponsKafkaProducer)
        val doneEventService = DoneInputEventService(metricsCollector, handleDuplicateEvents, doneEventDispatcher)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.doneInputTopicName, consumerProps, doneEventService)
    }

    private fun initializeInternBeskjedProducer(): Producer<NokkelIntern, BeskjedIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.BESKJEDINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.beskjedInternTopicName, kafkaProducer)
    }

    private fun initializeInternOppgaveProducer(): Producer<NokkelIntern, OppgaveIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.OPPGAVEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.oppgaveInternTopicName, kafkaProducer)
    }

    private fun initializeInternInnboksProducer(): Producer<NokkelIntern, InnboksIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.INNBOKSINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, InnboksIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.innboksInternTopicName, kafkaProducer)
    }

    private fun initializeInternDoneProducer(): Producer<NokkelIntern, DoneIntern> {
        val producerProps = Kafka.producerProps(environment, Eventtype.DONEINTERN)
        val kafkaProducer = KafkaProducer<NokkelIntern, DoneIntern>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.doneInternTopicName, kafkaProducer)
    }

    private fun initializeFeilresponsProducer(eventtype: Eventtype): Producer<NokkelFeilrespons, Feilrespons> {
        val producerProps = Kafka.producerFeilresponsProps(environment, eventtype)
        val kafkaProducer = KafkaProducer<NokkelFeilrespons, Feilrespons>(producerProps)
        kafkaProducer.initTransactions()
        return Producer(environment.feilresponsTopicName, kafkaProducer)
    }

    private fun initializeBeskjedRapidProducer() =
        BeskjedRapidProducer(
            KafkaProducer(
                Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.aivenBrokers)
                    put(ProducerConfig.CLIENT_ID_CONFIG, environment.groupId + "Beskjed" + NetUtil.getHostname(InetSocketAddress(0)))
                    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, String::class.java)
                    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, String::class.java)
                    put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
                    put(ProducerConfig.ACKS_CONFIG, "all")
                    put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
                    putAll(Kafka.credentialPropsAiven(environment.securityConfig.variables!!))
                }
            ),
            environment.rapidTopic
        )

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
