package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.TestData.oppgaveInput
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OppgaveInputTest {

    private val goodEvents = createEvents() + createOppgaveWithNullFields()
    private val badEvents = listOf(
        createEventWithInvalidEventId()
    )
    private val oppgaveEvents = goodEvents + badEvents

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
        oppgaveEvents.forEachIndexed { index, (key, value) ->
            kafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.oppgaveInputTopicName,
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
                offset = oppgaveEvents.size.toLong()
            )
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender validerte oppgaveer til intern-topic`() {
        val oppgaveAvroKey = oppgaveEvents.first().first
        val oppgaveAvroValue = oppgaveEvents.first().second

        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val oppgaveJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        oppgaveJson.has("@event_name") shouldBe true
        oppgaveJson["@event_name"].asText() shouldBe "opprett"
        oppgaveJson["type"].asText() shouldBe "oppgave"
        oppgaveJson["ident"].asText() shouldBe oppgaveAvroKey.fodselsnummer
        oppgaveJson["produsent"]["cluster"].asText() shouldBe "ukjent"
        oppgaveJson["produsent"]["namespace"].asText() shouldBe oppgaveAvroKey.namespace
        oppgaveJson["produsent"]["appnavn"].asText() shouldBe oppgaveAvroKey.appnavn
        oppgaveJson["varselId"].asText() shouldBe oppgaveAvroKey.eventId
        oppgaveJson["tekster"][0]["spraakkode"].asText() shouldBe "nb"
        oppgaveJson["tekster"][0]["tekst"].asText() shouldBe oppgaveAvroValue.tekst
        oppgaveJson["tekster"][0]["default"].asBoolean() shouldBe true
        oppgaveJson["link"].asText() shouldBe oppgaveAvroValue.link
        oppgaveJson["sensitivitet"].asText() shouldBe oppgaveAvroValue.sikkerhetsnivaa.toSensitivitetString()
        oppgaveJson["aktivFremTil"].asEpoch() shouldBe oppgaveAvroValue.synligFremTil
        if (oppgaveAvroValue.eksternVarsling) {
            oppgaveJson["eksternVarsling"]["prefererteKanaler"].map { it.asText() } shouldBe oppgaveAvroValue.prefererteKanaler
            oppgaveJson["eksternVarsling"]["smsVarslingstekst"].asText() shouldBe oppgaveAvroValue.smsVarslingstekst
            oppgaveJson["eksternVarsling"]["epostVarslingstekst"].asText() shouldBe oppgaveAvroValue.epostVarslingstekst
            oppgaveJson["eksternVarsling"]["epostVarslingstittel"].asText() shouldBe oppgaveAvroValue.epostVarslingstittel
        }
    }

    private fun createEvents() = (1..10).map {
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to oppgaveInput()
    }

    private fun createOppgaveWithNullFields() = listOf(
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = "123"
        ) to oppgaveInput(
            synligFremTil = null,
            smsVarslingstekst = null,
            epostVarslingstekst = null,
            epostVarslingstittel = null
        )
    )

    private fun createEventWithInvalidEventId() =
        NokkelTestData.createNokkelInputWithEventId("notUuidOrUlid") to oppgaveInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        NokkelTestData.createNokkelInputWithEventId(existingEventId) to oppgaveInput()
}
