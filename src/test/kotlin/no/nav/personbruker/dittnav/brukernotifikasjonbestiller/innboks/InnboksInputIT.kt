package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.getAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.asTimestamp
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks.InnboksTestData.innboksInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselRapidProducer
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
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )
    private val innboksVarsler = goodEvents + badEvents

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val varselForwarder = VarselForwarder(
        metricsCollector = metricsCollector,
        varselRapidProducer = VarselRapidProducer(rapidKafkaProducer, "rapid"),
        brukernotifikasjonbestillingRepository = brukernotifikasjonbestillingRepository
    )
    private val eventService = InnboksInputEventService(varselForwarder)

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

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.innboksInputTopicName, innboksVarsler.size.toLong())
            inputEventConsumer.stopPolling()
        }
    }


    @Test
    fun `Sender validerte innbokser til intern-topic`() {
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
        innboksJson["smsVarslingstekst"].asText() shouldBe innboksAvroValue.getSmsVarslingstekst()
        innboksJson["epostVarslingstekst"].asText() shouldBe innboksAvroValue.getEpostVarslingstekst()
        innboksJson["epostVarslingstittel"].asText() shouldBe innboksAvroValue.getEpostVarslingstittel()
    }

    @Test
    fun `Lagrer bestillingene i basen`() {
        runBlocking {
            val brukernotifikasjonbestillinger = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            brukernotifikasjonbestillinger.size shouldBe goodEvents.size

            val (beskjedKey, _) = goodEvents.first()
            brukernotifikasjonbestillinger.first {
                it.eventId == beskjedKey.getEventId()
            }.apply {
                eventtype shouldBe Eventtype.INNBOKS
                fodselsnummer shouldBe beskjedKey.getFodselsnummer()
            }
        }
    }
    private fun createEvents() = (1..10).map {
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to innboksInput()
    }

    private fun createInnboksWithNullFields() = listOf(
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to innboksInput(
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithInvalidEventId() =
        createNokkelInputWithEventId("notUuidOrUlid") to innboksInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        createNokkelInputWithEventId(existingEventId) to innboksInput()
}
