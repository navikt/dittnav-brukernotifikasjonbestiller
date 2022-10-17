package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.output.Feilrespons
import no.nav.brukernotifikasjon.schemas.output.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventDispatcher
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.HandleDuplicateEvents
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.asTimestamp
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Producer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.AvroInnboksInputObjectMother.createInnboksInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnboksInputIT {
    private val database = LocalPostgresDatabase.cleanDb()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents() + createInnboksWithNullFields()
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )
    private val innboksVarsler = goodEvents + badEvents

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, InnboksIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.innboksInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)
    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.INNBOKS, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = InnboksInputEventService(
        metricsCollector = metricsCollector,
        handleDuplicateEvents = handleDuplicateEvents,
        eventDispatcher = eventDispatcher,
        innboksRapidProducer = InnboksRapidProducer(rapidKafkaProducer, "rapid"),
        produceToRapid = true
    )

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, InnboksInput>(KafkaTestTopics.innboksInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.innboksInputTopicName, inputKafkaConsumer, eventService)

    @BeforeAll
    fun setup() {
        innboksVarsler.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.innboksInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        internalKafkaProducer.initTransactions()
        feilresponsKafkaProducer.initTransactions()
        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.innboksInputTopicName, innboksVarsler.size.toLong())
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Should read innboksvarsler and send to hoved-topic or error response topic as appropriate`() {
        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
    }

    @Test
    fun `Sender innboksvarsler på rapid-format`() {
        val innboksAvroKey = innboksVarsler.first().first
        val innboksAvroValue = innboksVarsler.first().second

        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val innboksJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        innboksJson.has("@event_name") shouldBe true
        innboksJson["@event_name"].asText() shouldBe "innboks"
        innboksJson["fodselsnummer"].asText() shouldBe innboksAvroKey.getFodselsnummer()
        innboksJson["namespace"].asText() shouldBe innboksAvroKey.getNamespace()
        innboksJson["appnavn"].asText() shouldBe innboksAvroKey.getAppnavn()
        innboksJson["eventId"].asText() shouldBe innboksAvroKey.getEventId()
        innboksJson["grupperingsId"].asText() shouldBe innboksAvroKey.getGrupperingsId()
        innboksJson["eventTidspunkt"].asTimestamp() shouldBe innboksAvroValue.getTidspunkt()
        innboksJson.has("forstBehandlet") shouldBe true
        innboksJson["tekst"].asText() shouldBe innboksAvroValue.getTekst()
        innboksJson["link"].asText() shouldBe innboksAvroValue.getLink()
        innboksJson["sikkerhetsnivaa"].asInt() shouldBe innboksAvroValue.getSikkerhetsnivaa()
        innboksJson["aktiv"].asBoolean() shouldBe true
        innboksJson["eksternVarsling"].asBoolean() shouldBe innboksAvroValue.getEksternVarsling()
        innboksJson["prefererteKanaler"].map { it.asText() } shouldBe innboksAvroValue.getPrefererteKanaler()
    }

    @Test
    fun `Sender innboksvarsler til rapid i tillegg til gammelt topic`() {
        internalKafkaProducer.history().size shouldBe goodEvents.size
        rapidKafkaProducer.history().size shouldBe goodEvents.size
    }

    private fun createEvents() = (1..10).map {
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to createInnboksInput()
    }

    private fun createInnboksWithNullFields() = listOf(
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to createInnboksInput(
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithTooLongGroupId(): Pair<NokkelInput, InnboksInput> =
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "groupId".repeat(100)
        ) to createInnboksInput()

    private fun createEventWithInvalidEventId() =
        createNokkelInputWithEventId("notUuidOrUlid") to createInnboksInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        createNokkelInputWithEventId(existingEventId) to createInnboksInput()
}
