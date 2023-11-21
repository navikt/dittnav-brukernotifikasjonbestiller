package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.polling.PeriodicConsumerPollingCheck
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneRapidProducer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveInputEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselRapidProducer
import no.nav.brukernotifikasjon.schemas.input.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ApplicationContext {

    private val log = KotlinLogging.logger {  }

    val environment = Environment()
    val database: Database = PostgresDatabase(environment)
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)

    private val metricsCollector = MetricsCollector()

    private val rapidKafkaProducer = initializeRapidKafkaProducer()
    val doneRapidProducer = DoneRapidProducer(
        kafkaProducer = rapidKafkaProducer,
        topicName = environment.rapidTopic
    )
    val varselRapidProducer = VarselRapidProducer(
        kafkaProducer = rapidKafkaProducer,
        topicName = environment.rapidTopic
    )

    private val varselForwarder = VarselForwarder(
        metricsCollector = metricsCollector,
        varselRapidProducer = varselRapidProducer,
        brukernotifikasjonbestillingRepository = brukernotifikasjonbestillingRepository
    )

    var beskjedInputConsumer = initializeBeskjedInputProcessor()
    var oppgaveInputConsumer = initializeOppgaveInputProcessor()
    var innboksInputConsumer = initializeInnboksInputProcessor()
    var doneInputConsumer = initializeDoneInputProcessor()

    var periodicConsumerPollingCheck = initializePeriodicConsumerPollingCheck()

    private fun initializeBeskjedInputProcessor(): Consumer<NokkelInput, BeskjedInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val beskjedEventService = BeskjedInputEventService(varselForwarder)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.beskjedInputTopicName, consumerProps, beskjedEventService)
    }

    private fun initializeOppgaveInputProcessor(): Consumer<NokkelInput, OppgaveInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val oppgaveEventService = OppgaveInputEventService(varselForwarder)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.oppgaveInputTopicName, consumerProps, oppgaveEventService)
    }

    private fun initializeInnboksInputProcessor(): Consumer<NokkelInput, InnboksInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.INNBOKS)
        val innboksEventService = InnboksInputEventService(varselForwarder,)
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.innboksInputTopicName, consumerProps, innboksEventService)
    }

    private fun initializeDoneInputProcessor(): Consumer<NokkelInput, DoneInput> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val doneEventService = DoneInputEventService(
            metricsCollector,
            doneRapidProducer,
            brukernotifikasjonbestillingRepository
        )
        return KafkaConsumerSetup.setUpConsumerForInputTopic(environment.doneInputTopicName, consumerProps, doneEventService)
    }

    private fun initializeRapidKafkaProducer() = KafkaProducer<String, String>(
        Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.aivenBrokers)
            put(
                ProducerConfig.CLIENT_ID_CONFIG,
                "dittnav-brukernotifikasjonbestiller"
            )
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
            putAll(Kafka.credentialPropsAiven(environment.securityConfig.variables!!))
        }
    )

    private fun initializePeriodicConsumerPollingCheck(): PeriodicConsumerPollingCheck {
        return PeriodicConsumerPollingCheck(this)
    }

    fun reinitializeConsumers() {
        if (beskjedInputConsumer.isCompleted()) {
            beskjedInputConsumer = initializeBeskjedInputProcessor()
            log.info { "beskjedInputConsumer har blitt reinstansiert." }
        } else {
            log.warn { "beskjedInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv." }
        }

        if (oppgaveInputConsumer.isCompleted()) {
            oppgaveInputConsumer = initializeOppgaveInputProcessor()
            log.info { "oppgaveInputConsumer har blitt reinstansiert." }
        } else {
            log.warn { "oppgaveInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv." }
        }

        if (innboksInputConsumer.isCompleted()) {
            innboksInputConsumer = initializeInnboksInputProcessor()
            log.info { "innboksInputConsumer har blitt reinstansiert." }
        } else {
            log.warn { "innboksInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv." }
        }

        if (doneInputConsumer.isCompleted()) {
            doneInputConsumer = initializeDoneInputProcessor()
            log.info { "doneInputConsumer har blitt reinstansiert." }
        } else {
            log.warn { "doneInputConsumer kunne ikke bli reinstansiert fordi den fortsatt er aktiv." }
        }
    }
}
