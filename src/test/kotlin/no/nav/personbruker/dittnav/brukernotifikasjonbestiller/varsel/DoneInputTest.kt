package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.TestData.doneInput
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoneInputTest {

    private val doneEvents = createEvents()

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
        doneEvents.forEachIndexed { index, (key, value) ->
            kafkaConsumer.addRecord(
                ConsumerRecord(
                    KafkaTestTopics.doneInputTopicName,
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
                offset = doneEvents.size.toLong()
            )
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender validerte doneer til intern-topic`() {
        val doneAvroKey = doneEvents.first().first

        rapidKafkaProducer.history().size shouldBe doneEvents.size

        val doneJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        doneJson.has("@event_name") shouldBe true
        doneJson["@event_name"].asText() shouldBe "inaktiver"
        doneJson["produsent"]["cluster"].asText() shouldBe "ukjent"
        doneJson["produsent"]["namespace"].asText() shouldBe doneAvroKey.namespace
        doneJson["produsent"]["appnavn"].asText() shouldBe doneAvroKey.appnavn
        doneJson["varselId"].asText() shouldBe doneAvroKey.eventId
    }

    private fun createEvents() = (1..10).map {
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to doneInput()
    }
}
