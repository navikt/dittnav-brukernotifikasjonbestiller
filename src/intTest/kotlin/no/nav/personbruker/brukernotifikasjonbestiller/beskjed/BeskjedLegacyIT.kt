package no.nav.personbruker.brukernotifikasjonbestiller.beskjed

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.brukernotifikasjonbestiller.CapturingEventProcessor
import no.nav.personbruker.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.brukernotifikasjonbestiller.common.getClient
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedLegacyObjectMother.createBeskjedLegacyWithGrupperingsId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyEventService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedLegacyTransformer
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
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URL

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeskjedLegacyIT {
    private val database = LocalPostgresDatabase()

    private val capturedInternalRecords = ArrayList<RecordKeyValueWrapper<NokkelIntern, BeskjedIntern>>()
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
    private val beskjedTransformer = BeskjedLegacyTransformer(mapper)
    private val feilresponsTransformer = FeilresponsLegacyTransformer(mapper)

    private val goodEvents = createEvents(10)
    private val badEvents = listOf(createEventWithTooLongGroupId("bad"))
    private val beskjedEvents = goodEvents.toMutableList().apply {
        addAll(badEvents)
    }.toMap()

    private val beskedLegacyConsumerMock = KafkaTestUtil.createMockConsumer<Nokkel, Beskjed>(KafkaTestTopics.beskjedLegacyTopicName)

    private val beskjedInternalProducerMock = KafkaTestUtil.createMockProducer<NokkelIntern, BeskjedIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.beskjedInternTopicName, beskjedInternalProducerMock)

    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)

    val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    val handleDuplicateEvents = HandleDuplicateEventsLegacy(Eventtype.BESKJED, brukernotifikasjonbestillingRepository)
    val eventDispatcher = EventDispatcher(Eventtype.BESKJED, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)

    val eventService = BeskjedLegacyEventService(beskjedTransformer, feilresponsTransformer, metricsCollector, handleDuplicateEvents, eventDispatcher)
    val consumer = Consumer(KafkaTestTopics.beskjedLegacyTopicName, beskedLegacyConsumerMock, eventService)


    @Test
    fun `Should read Beskjed-events and send to hoved-topic or error response topic as appropriate`() {
        var i = 0
        beskjedEvents.forEach {
            beskedLegacyConsumerMock.addRecord(
                ConsumerRecord(
                KafkaTestTopics.beskjedLegacyTopicName,
                0,
                (i++).toLong(),
                it.key,
                it.value
            )
            )
        }

        `Read all Beskjed-events from our legacy-topic and verify that they have been sent to the main-topic`()

        capturedInternalRecords.size `should be equal to` goodEvents.size
        capturedErrorResponseRecords.size `should be equal to` badEvents.size
    }


    fun `Read all Beskjed-events from our legacy-topic and verify that they have been sent to the main-topic`() {

        beskjedInternalProducerMock.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            consumer.startPolling()

            KafkaTestUtil.delayUntilCommittedOffset(beskedLegacyConsumerMock, KafkaTestTopics.beskjedLegacyTopicName, beskjedEvents.size.toLong())
            `Wait until all beskjed events have been received by target topic`()
            `Wait until bad event has been received by error topic`()

            consumer.stopPolling()
        }
    }

    private fun `Wait until all beskjed events have been received by target topic`() {
        val targetKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelIntern, BeskjedIntern>(KafkaTestTopics.beskjedInternTopicName)
        KafkaTestUtil.loopbackRecords(beskjedInternalProducerMock, targetKafkaConsumer)

        val capturingProcessor = CapturingEventProcessor<NokkelIntern, BeskjedIntern>()
        val targetConsumer = Consumer(KafkaTestTopics.beskjedInternTopicName, targetKafkaConsumer, capturingProcessor)

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
        createNokkelLegacyWithEventIdAndSystembruker(it.toString(), producerServiceUser) to createBeskjedLegacyWithGrupperingsId(it.toString())
    }

    private fun createEventWithTooLongGroupId(eventId: String): Pair<Nokkel, Beskjed> {
        val groupId = "groupId".repeat(100)

        return createNokkelLegacyWithEventIdAndSystembruker(eventId, producerServiceUser) to createBeskjedLegacyWithGrupperingsId(groupId)
    }
}
