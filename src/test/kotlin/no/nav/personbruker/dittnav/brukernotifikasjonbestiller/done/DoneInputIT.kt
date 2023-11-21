package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.tms.brukernotifikasjon.schemas.input.DoneInput
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingRepository
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.getAllBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.LocalPostgresDatabase
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.Consumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestTopics
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.DoneTestData.doneInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.MetricsCollector
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoneInputIT {
    private val database = LocalPostgresDatabase.cleanDb()
    private val metricsCollector = MetricsCollector()

    private val goodEvents = createEvents()
    private val badEvents = listOf(
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )

    private val doneEvents = goodEvents + badEvents

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val eventService = DoneInputEventService(
        metricsCollector = metricsCollector,
        doneRapidProducer = DoneRapidProducer(rapidKafkaProducer, "rapid"),
        brukernotifikasjonbestillingRepository = brukernotifikasjonbestillingRepository,
    )

    private val inputKafkaConsumer = KafkaTestUtil.createMockConsumer<NokkelInput, DoneInput>(KafkaTestTopics.doneInputTopicName)
    private val inputEventConsumer = Consumer(KafkaTestTopics.doneInputTopicName, inputKafkaConsumer, eventService)

    @BeforeAll
    fun setup() {
        doneEvents.forEachIndexed { index, (key, value) ->
            inputKafkaConsumer.addRecord(ConsumerRecord(
                KafkaTestTopics.doneInputTopicName,
                0,
                index.toLong(),
                key,
                value
            ))
        }

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.doneInputTopicName, doneEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender done på rapid-format`() {
        rapidKafkaProducer.history().size shouldBe goodEvents.size

        val doneAvroKey = doneEvents.first().first
        val doneJson = ObjectMapper().readTree(rapidKafkaProducer.history().first().value())
        doneJson.has("@event_name") shouldBe true
        doneJson["@event_name"].asText() shouldBe "done"
        doneJson["fodselsnummer"].asText() shouldBe doneAvroKey.getFodselsnummer()
        doneJson["eventId"].asText() shouldBe doneAvroKey.getEventId()
        doneJson.has("forstBehandlet") shouldBe true
    }

    @Test
    fun `Lagrer done i basen`() {
        runBlocking {
            val brukernotifikasjonbestillinger = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            brukernotifikasjonbestillinger.size shouldBe goodEvents.size

            val (oppgaveKey, _) = goodEvents.first()
            brukernotifikasjonbestillinger.first {
                it.eventId == oppgaveKey.getEventId()
            }.apply {
                eventtype shouldBe Eventtype.DONE
                fodselsnummer shouldBe oppgaveKey.getFodselsnummer()
            }
        }
    }

    private fun createEvents() = (1..10).map {
        NokkelTestData.createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to doneInput()
    }

    private fun createEventWithInvalidEventId(): Pair<NokkelInput, DoneInput> =
        NokkelTestData.createNokkelInputWithEventId("notUuidOrUlid") to doneInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        NokkelTestData.createNokkelInputWithEventId(existingEventId) to doneInput()
}
