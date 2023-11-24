package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.TestData.beskjedInput
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BeskjedInputTest {

    private val goodEvents = createEvents() + createBeskjedWithNullFields()
    private val badEvents = listOf(
        createEventWithInvalidEventId(),
    )
    private val beskjedEvents = goodEvents + badEvents

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
        beskjedEvents.forEachIndexed { index, (key, value) ->
            kafkaConsumer.addRecord(
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
            KafkaTestUtil.delayUntilTotalOffset(
                consumer = kafkaConsumer,
                topics = KafkaTestTopics.inputTopics,
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
        beskjedJson["@event_name"].asText() shouldBe "opprett"
        beskjedJson["type"].asText() shouldBe "beskjed"
        beskjedJson["ident"].asText() shouldBe beskjedAvroKey.fodselsnummer
        beskjedJson["produsent"]["cluster"].asText() shouldBe "ukjent"
        beskjedJson["produsent"]["namespace"].asText() shouldBe beskjedAvroKey.namespace
        beskjedJson["produsent"]["appnavn"].asText() shouldBe beskjedAvroKey.appnavn
        beskjedJson["varselId"].asText() shouldBe beskjedAvroKey.eventId
        beskjedJson["tekster"][0]["spraakkode"].asText() shouldBe "nb"
        beskjedJson["tekster"][0]["tekst"].asText() shouldBe beskjedAvroValue.tekst
        beskjedJson["tekster"][0]["default"].asBoolean() shouldBe true
        beskjedJson["link"].asText() shouldBe beskjedAvroValue.link
        beskjedJson["sensitivitet"].asText() shouldBe beskjedAvroValue.sikkerhetsnivaa.toSensitivitetString()
        beskjedJson["aktivFremTil"].asEpoch() shouldBe beskjedAvroValue.synligFremTil
        if (beskjedAvroValue.eksternVarsling) {
            beskjedJson["eksternVarsling"]["prefererteKanaler"].map { it.asText() } shouldBe beskjedAvroValue.prefererteKanaler
            beskjedJson["eksternVarsling"]["smsVarslingstekst"].asText() shouldBe beskjedAvroValue.smsVarslingstekst
            beskjedJson["eksternVarsling"]["epostVarslingstekst"].asText() shouldBe beskjedAvroValue.epostVarslingstekst
            beskjedJson["eksternVarsling"]["epostVarslingstittel"].asText() shouldBe beskjedAvroValue.epostVarslingstittel
        }
    }

    private fun createEvents() = (1..10).map {
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to beskjedInput()
    }

    private fun createBeskjedWithNullFields() = listOf(
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
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
        NokkelTestData.createNokkelInputWithEventId("notUuidOrUlid") to beskjedInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        NokkelTestData.createNokkelInputWithEventId(existingEventId) to beskjedInput()
}
