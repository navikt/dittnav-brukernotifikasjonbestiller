package no.nav.personbruker.brukernotifikasjonbestiller.statusoppdatering

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEventsLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons.FeilresponsLegacyTransformer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollectorLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameResolver
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.ProducerNameScrubber
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventIdAndSystembruker
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.AvroStatusoppdateringLegacyObjectMother.createStatusoppdateringLegacyWithGrupperingsId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering.StatusoppdateringLegacyTransformer
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatusoppdateringLegacyIT {
    private val database = LocalPostgresDatabase()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, StatusoppdateringIntern>>()
    private val capturedErrorResponseRecords = ArrayList<RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons>>()

    private val producerNameAlias = "dittnav"
    private val client = getClient(producerNameAlias)
    private val metricsReporter = StubMetricsReporter()
    private val nameResolver = ProducerNameResolver(client, URL("http://event-handler"))
    private val nameScrubber = ProducerNameScrubber(nameResolver)
    private val metricsCollector = MetricsCollectorLegacy(metricsReporter, nameScrubber)
    private val producerServiceUser = "dummySystembruker"
    private val producerNamespace = "namespace"
    private val producerAppName = "appName"
    private val mapper = ServiceUserMapper(mapOf(producerServiceUser to NamespaceAppName(producerNamespace, producerAppName)))
    private val statusoppdateringTransformer = StatusoppdateringLegacyTransformer(mapper)
    private val feilresponsTransformer = FeilresponsLegacyTransformer(mapper)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(createEventWithTooLongGroupId("bad"))
    private val statusoppdateringEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, StatusoppdateringIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.statusoppdateringInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.STATUSOPPDATERING, brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = StatusoppdateringLegacyEventService(statusoppdateringTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)

    private val legacyKafkaConsumer = KafkaTestUtil.createMockConsumer<Nokkel, Statusoppdatering>(KafkaTestTopics.statusoppdateringLegacyTopicName)
    private val legacyEvnetConsumer = Consumer(KafkaTestTopics.statusoppdateringLegacyTopicName, legacyKafkaConsumer, eventService)

    @Test
    fun `Should read Statusoppdatering-events and send to hoved-topic or error response topic as appropriate`() {
        var i = 0
        statusoppdateringEvents.forEach {
            legacyKafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.statusoppdateringLegacyTopicName,
                    0,
                    (i++).toLong(),
                    it.key,
                    it.value
                )
            )
        }

        `Read all Statusoppdatering-events from our legacy-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Statusoppdatering-events from our legacy-topic and verify that they have been sent to the main-topic`() {

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            legacyEvnetConsumer.startPolling()

            KafkaTestUtil.delayUntilCommittedOffset(legacyKafkaConsumer, KafkaTestTopics.statusoppdateringLegacyTopicName, statusoppdateringEvents.size.toLong())
            `Wait until all statusoppdatering events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            legacyEvnetConsumer.stopPolling()
        }
    }

    private fun `Wait until all statusoppdatering events have been received by target topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelIntern, StatusoppdateringIntern>(KafkaTestTopics.statusoppdateringInternTopicName)
        KafkaTestUtil.loopbackRecords(internalKafkaProducer, targetKafkaConsumer)

        val capturingProcessor = CapturingEventProcessor<NokkelIntern, StatusoppdateringIntern>()
        val targetConsumer = Consumer(KafkaTestTopics.oppgaveInternTopicName, targetKafkaConsumer, capturingProcessor)

        var currentNumberOfRecords = 0
        targetConsumer.startPolling()
        while (currentNumberOfRecords < goodEvents.size) {
            runBlocking {
                currentNumberOfRecords = capturingProcessor.getEvents().size
                delay(100)
            }
        }
        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedInternalRecords.addAll(capturingProcessor.getEvents())
    }


    private fun `Wait until bad event has been received by error topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelFeilrespons, Feilrespons>(KafkaTestTopics.feilresponsTopicName)
        KafkaTestUtil.loopbackRecords(feilresponsKafkaProducer, targetKafkaConsumer)

        val capturingProcessor = CapturingEventProcessor<NokkelFeilrespons, Feilrespons>()
        val targetConsumer = Consumer(KafkaTestTopics.feilresponsTopicName, targetKafkaConsumer, capturingProcessor)

        var receivedEvent = false
        targetConsumer.startPolling()
        while (!receivedEvent) {
            runBlocking {
                receivedEvent = capturingProcessor.getEvents().isNotEmpty()
                delay(100)
            }
        }
        runBlocking {
            targetConsumer.stopPolling()
        }

        capturedErrorResponseRecords.addAll(capturingProcessor.getEvents())
    }

    private fun createEvents(number: Int) = (1..number).map {
        createNokkelLegacyWithEventIdAndSystembruker(it.toString(), producerServiceUser) to createStatusoppdateringLegacyWithGrupperingsId(it.toString())
    }

    private fun createEventWithTooLongGroupId(eventId: String): Pair<Nokkel, Statusoppdatering> {
        val groupId = "groupId".repeat(100)

        return createNokkelLegacyWithEventIdAndSystembruker(eventId, producerServiceUser) to createStatusoppdateringLegacyWithGrupperingsId(groupId)
    }
}
