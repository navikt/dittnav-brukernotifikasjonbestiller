package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData.beskjedInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.getAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.asTimestamp
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData.createNokkelInputWithEventId
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData.createNokkelInputWithEventIdAndGroupId
import no.nav.personbruker.dittnav.common.metrics.StubMetricsReporter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeskjedInputIT {

    private val database = LocalPostgresDatabase.cleanDb()
    private val metricsReporter = StubMetricsReporter()
    private val metricsCollector = MetricsCollector(metricsReporter)

    private val goodEvents = createEvents() + createBeskjedWithNullFields()
    private val badEvents = listOf(
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )
    private val beskjedEvents = goodEvents + badEvents

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()

    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val eventService = BeskjedInputEventService(
        metricsCollector = metricsCollector,
        beskjedRapidProducer = BeskjedRapidProducer(rapidKafkaProducer, "rapid"),
        brukernotifikasjonbestillingRepository = brukernotifikasjonbestillingRepository,
    )

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, BeskjedInput>(KafkaTestTopics.beskjedInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.beskjedInputTopicName, inputKafkaConsumer, eventService)

    @BeforeAll
    fun setup() {
        beskjedEvents.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.beskjedInputTopicName,
                    0,
                    index.toLong(),
                    key,
                    value
                )
            )
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(
                consumer = inputKafkaConsumer,
                topicName = KafkaTestTopics.beskjedInputTopicName,
                offset = beskjedEvents.size.toLong()
            )
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender validerte beskjeder til intern-topic`() {
        val beskjedAvroKey = beskjedEvents.first().first
        val beskjedAvroValue = beskjedEvents.first().second

        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val beskjedJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        beskjedJson.has("@event_name") shouldBe true
        beskjedJson["@event_name"].asText() shouldBe "beskjed"
        beskjedJson["fodselsnummer"].asText() shouldBe beskjedAvroKey.getFodselsnummer()
        beskjedJson["namespace"].asText() shouldBe beskjedAvroKey.getNamespace()
        beskjedJson["appnavn"].asText() shouldBe beskjedAvroKey.getAppnavn()
        beskjedJson["eventId"].asText() shouldBe beskjedAvroKey.getEventId()
        beskjedJson["grupperingsId"].asText() shouldBe beskjedAvroKey.getGrupperingsId()
        beskjedJson["eventTidspunkt"].asTimestamp() shouldBe beskjedAvroValue.getTidspunkt()
        beskjedJson.has("forstBehandlet") shouldBe true
        beskjedJson["tekst"].asText() shouldBe beskjedAvroValue.getTekst()
        beskjedJson["link"].asText() shouldBe beskjedAvroValue.getLink()
        beskjedJson["sikkerhetsnivaa"].asInt() shouldBe beskjedAvroValue.getSikkerhetsnivaa()
        beskjedJson["synligFremTil"].asTimestamp() shouldBe beskjedAvroValue.getSynligFremTil()
        beskjedJson["aktiv"].asBoolean() shouldBe true
        beskjedJson["eksternVarsling"].asBoolean() shouldBe beskjedAvroValue.getEksternVarsling()
        beskjedJson["prefererteKanaler"].map { it.asText() } shouldBe beskjedAvroValue.getPrefererteKanaler()
        beskjedJson["smsVarslingstekst"].asText() shouldBe beskjedAvroValue.getSmsVarslingstekst()
        beskjedJson["epostVarslingstekst"].asText() shouldBe beskjedAvroValue.getEpostVarslingstekst()
        beskjedJson["epostVarslingstittel"].asText() shouldBe beskjedAvroValue.getEpostVarslingstittel()
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
                eventtype shouldBe Eventtype.BESKJED
                fodselsnummer shouldBe beskjedKey.getFodselsnummer()
            }
        }
    }

    private fun createEvents() = (1..10).map {
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to beskjedInput()
    }

    private fun createBeskjedWithNullFields() = listOf(
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to beskjedInput(
            synligFremTil = null,
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithInvalidEventId() =
        createNokkelInputWithEventId("notUuidOrUlid") to beskjedInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        createNokkelInputWithEventId(existingEventId) to beskjedInput()
}
