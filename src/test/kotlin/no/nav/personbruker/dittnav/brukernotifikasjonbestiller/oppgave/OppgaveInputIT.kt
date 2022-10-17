package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.AvroOppgaveInputObjectMother.createOppgaveInput
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveInputIT {
    private val database = LocalPostgresDatabase.cleanDb()

    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents() + createOppgaveWithNullFields()
    private val badEvents = listOf(
        createEventWithTooLongGroupId(),
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )
    private val oppgaveEvents = goodEvents + badEvents

    private val internalKafkaProducer = KafkaTestUtil.createMockProducer<NokkelIntern, OppgaveIntern>()
    private val internalEventProducer = Producer(KafkaTestTopics.oppgaveInternTopicName, internalKafkaProducer)
    private val feilresponsKafkaProducer = KafkaTestUtil.createMockProducer<NokkelFeilrespons, Feilrespons>()
    private val feilresponsEventProducer = Producer(KafkaTestTopics.feilresponsTopicName, feilresponsKafkaProducer)
    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val handleDuplicateEvents = HandleDuplicateEvents(brukernotifikasjonbestillingRepository)
    private val eventDispatcher = EventDispatcher(Eventtype.OPPGAVE, brukernotifikasjonbestillingRepository, internalEventProducer, feilresponsEventProducer)
    private val eventService = OppgaveInputEventService(
        metricsCollector = metricsCollector,
        handleDuplicateEvents = handleDuplicateEvents,
        eventDispatcher = eventDispatcher,
        oppgaveRapidProducer = OppgaveRapidProducer(rapidKafkaProducer, "rapid"),
        produceToRapid = true
    )

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, OppgaveInput>(KafkaTestTopics.oppgaveInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.oppgaveInputTopicName, inputKafkaConsumer, eventService)

    @BeforeAll
    fun setup() {
        oppgaveEvents.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.oppgaveInputTopicName,
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
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.oppgaveInputTopicName, oppgaveEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Should read Oppgave-events and send to hoved-topic or error response topic as appropriate`() {
        internalKafkaProducer.history().size shouldBe goodEvents.size
        feilresponsKafkaProducer.history().size shouldBe badEvents.size
    }

    @Test
    fun `Sender oppgaver på rapid-format`() {
        val oppgaveAvroKey = oppgaveEvents.first().first
        val oppgaveAvroValue = oppgaveEvents.first().second

        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val oppgaveJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        oppgaveJson.has("@event_name") shouldBe true
        oppgaveJson["@event_name"].asText() shouldBe "oppgave"
        oppgaveJson["fodselsnummer"].asText() shouldBe oppgaveAvroKey.getFodselsnummer()
        oppgaveJson["namespace"].asText() shouldBe oppgaveAvroKey.getNamespace()
        oppgaveJson["appnavn"].asText() shouldBe oppgaveAvroKey.getAppnavn()
        oppgaveJson["eventId"].asText() shouldBe oppgaveAvroKey.getEventId()
        oppgaveJson["grupperingsId"].asText() shouldBe oppgaveAvroKey.getGrupperingsId()
        oppgaveJson["eventTidspunkt"].asTimestamp() shouldBe oppgaveAvroValue.getTidspunkt()
        oppgaveJson.has("forstBehandlet") shouldBe true
        oppgaveJson["tekst"].asText() shouldBe oppgaveAvroValue.getTekst()
        oppgaveJson["link"].asText() shouldBe oppgaveAvroValue.getLink()
        oppgaveJson["sikkerhetsnivaa"].asInt() shouldBe oppgaveAvroValue.getSikkerhetsnivaa()
        oppgaveJson["synligFremTil"].asTimestamp() shouldBe oppgaveAvroValue.getSynligFremTil()
        oppgaveJson["aktiv"].asBoolean() shouldBe true
        oppgaveJson["eksternVarsling"].asBoolean() shouldBe oppgaveAvroValue.getEksternVarsling()
        oppgaveJson["prefererteKanaler"].map { it.asText() } shouldBe oppgaveAvroValue.getPrefererteKanaler()
    }

    @Test
    fun `Sender oppgaver til rapid i tillegg til gammelt topic`() {
        internalKafkaProducer.history().size shouldBe goodEvents.size
        rapidKafkaProducer.history().size shouldBe goodEvents.size
    }

    private fun createEvents() = (1..10).map {
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to createOppgaveInput()
    }

    private fun createOppgaveWithNullFields() = listOf(
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to createOppgaveInput(
            synligFremTil = null,
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithTooLongGroupId() =
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "groupId".repeat(100)
        ) to createOppgaveInput()

    private fun createEventWithInvalidEventId() =
        createNokkelInputWithEventId( "notUuidOrUlid") to createOppgaveInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        createNokkelInputWithEventId(existingEventId) to createOppgaveInput()
}
