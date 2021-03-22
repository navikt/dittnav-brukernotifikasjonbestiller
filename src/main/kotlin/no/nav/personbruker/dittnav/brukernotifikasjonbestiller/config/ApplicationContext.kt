package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.brukernotifikasjon.schemas.*
import no.nav.brukernotifikasjon.schemas.internal.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedEventService
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

    private val httpClient = HttpClientBuilder.build()
    private val nameResolver = ProducerNameResolver(httpClient, environment.eventHandlerURL)
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsReporter = resolveMetricsReporter(environment)
    private val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

    var beskjedConsumer = initializeBeskjedProcessor()
    var oppgaveConsumer = initializeOppgaveProcessor()
    var statusoppdateringConsumer = initializeStatusoppdateringProcessor()
    var doneConsumer = initializeDoneProcessor()
    var feilresponsKafkaProducerWrapper = initializeFeilresponsKafkaProducer()

    private fun initializeBeskjedProcessor(): Consumer<Nokkel, Beskjed> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.BESKJED)
        val producerProps = Kafka.producerProps(environment)
        val internalKafkaProducerWrapper = KafkaProducerWrapper(Kafka.beskjedMainTopicName, KafkaProducer<NokkelIntern, BeskjedIntern>(producerProps))
        val beskjedEventProcessor = BeskjedEventService(internalKafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector)
        return KafkaConsumerSetup.setupConsumerForTheBeskjedInputTopic(consumerProps, beskjedEventProcessor)
    }

    private fun initializeOppgaveProcessor(): Consumer<Nokkel, Oppgave> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.OPPGAVE)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.oppgaveMainTopicName, KafkaProducer<NokkelIntern, OppgaveIntern>(producerProps))
        val oppgaveEventProcessor = OppgaveEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector)
        return KafkaConsumerSetup.setupConsumerForTheOppgaveInputTopic(consumerProps, oppgaveEventProcessor)
    }

    private fun initializeStatusoppdateringProcessor(): Consumer<Nokkel, Statusoppdatering> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.STATUSOPPDATERING)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.statusoppdateringMainTopicName, KafkaProducer<NokkelIntern, StatusoppdateringIntern>(producerProps))
        val statusoppdateringEventProcessor = StatusoppdateringEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector)
        return KafkaConsumerSetup.setupConsumerForTheStatusoppdateringInputTopic(consumerProps, statusoppdateringEventProcessor)
    }

    private fun initializeDoneProcessor(): Consumer<Nokkel, Done> {
        val consumerProps = Kafka.consumerProps(environment, Eventtype.DONE)
        val producerProps = Kafka.producerProps(environment)
        val kafkaProducerWrapper = KafkaProducerWrapper(Kafka.doneMainTopicName, KafkaProducer<NokkelIntern, DoneIntern>(producerProps))
        val doneEventProcessor = DoneEventService(kafkaProducerWrapper, feilresponsKafkaProducerWrapper, metricsCollector)
        return KafkaConsumerSetup.setupConsumerForTheDoneInputTopic(consumerProps, doneEventProcessor)
    }

    private fun initializeFeilresponsKafkaProducer(): KafkaProducerWrapper<NokkelFeilrespons, Feilrespons> {
        val feilresponsProducerProps = Kafka.producerProps(environment)
        return KafkaProducerWrapper(Kafka.feilresponsTopicName, KafkaProducer<NokkelFeilrespons, Feilrespons>(feilresponsProducerProps))
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

        feilresponsKafkaProducerWrapper = initializeFeilresponsKafkaProducer()
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
