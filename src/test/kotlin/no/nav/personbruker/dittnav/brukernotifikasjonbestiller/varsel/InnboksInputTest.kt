package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.TestData.innboksInput
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InnboksInputTest {

    private val goodEvents = createEvents() + createInnboksWithNullFields()
    private val badEvents = listOf(
        createEventWithInvalidEventId()
    )
    private val innboksEvents = goodEvents + badEvents

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()

    private val varselForwarder = VarselActionForwarder(
        varselActionProducer = VarselActionProducer(rapidKafkaProducer, "aapen-brukervarsel"),
    )
    private val kafkaConsumer =
        KafkaTestUtil.createMockConsumer<NokkelInput, GenericRecord>(KafkaTestTopics.inputTopics)

    private val inputEventConsumer =
        InputTopicConsumer(KafkaTestTopics.inputTopics, kafkaConsumer, varselForwarder)

    @BeforeAll
    fun setup() {
        innboksEvents.forEachIndexed { index, (key, value) ->
            kafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.innboksInputTopicName,
                    0,
                    index.toLong(),
                    key,
                    value
                )
            )
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilTotalOffset(
                consumer = kafkaConsumer,
                topics = KafkaTestTopics.inputTopics,
                offset = innboksEvents.size.toLong()
            )
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender validerte innbokser til intern-topic`() {
        val innboksAvroKey = innboksEvents.first().first
        val innboksAvroValue = innboksEvents.first().second

        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val innboksJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        innboksJson.has("@event_name") shouldBe true
        innboksJson["@event_name"].asText() shouldBe "opprett"
        innboksJson["type"].asText() shouldBe "innboks"
        innboksJson["ident"].asText() shouldBe innboksAvroKey.fodselsnummer
        innboksJson["produsent"]["cluster"].asText() shouldBe "ukjent"
        innboksJson["produsent"]["namespace"].asText() shouldBe innboksAvroKey.namespace
        innboksJson["produsent"]["appnavn"].asText() shouldBe innboksAvroKey.appnavn
        innboksJson["varselId"].asText() shouldBe innboksAvroKey.eventId
        innboksJson["tekster"][0]["spraakkode"].asText() shouldBe "nb"
        innboksJson["tekster"][0]["tekst"].asText() shouldBe innboksAvroValue.tekst
        innboksJson["tekster"][0]["default"].asBoolean() shouldBe true
        innboksJson["link"].asText() shouldBe innboksAvroValue.link
        innboksJson["sensitivitet"].asText() shouldBe innboksAvroValue.sikkerhetsnivaa.toSensitivitetString()
        if (innboksAvroValue.eksternVarsling) {
            innboksJson["eksternVarsling"]["prefererteKanaler"].map { it.asText() } shouldBe innboksAvroValue.prefererteKanaler
            innboksJson["eksternVarsling"]["smsVarslingstekst"].asText() shouldBe innboksAvroValue.smsVarslingstekst
            innboksJson["eksternVarsling"]["epostVarslingstekst"].asText() shouldBe innboksAvroValue.epostVarslingstekst
            innboksJson["eksternVarsling"]["epostVarslingstittel"].asText() shouldBe innboksAvroValue.epostVarslingstittel
        }
    }

    private fun createEvents() = (1..10).map {
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to innboksInput()
    }

    private fun createInnboksWithNullFields() = listOf(
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to innboksInput(
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithInvalidEventId() =
        NokkelTestData.createNokkelInputWithEventId("notUuidOrUlid") to innboksInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        NokkelTestData.createNokkelInputWithEventId(existingEventId) to innboksInput()
}
