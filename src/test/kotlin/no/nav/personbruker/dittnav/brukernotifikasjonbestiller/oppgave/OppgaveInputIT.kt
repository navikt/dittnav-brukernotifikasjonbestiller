package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
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
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave.OppgaveTestData.oppgaveInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselForwarder
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselRapidProducer
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
        createEventWithInvalidEventId(),
        createEventWithDuplicateId(goodEvents.first().first.getEventId())
    )
    private val oppgaveEvents = goodEvents + badEvents

    private val rapidKafkaProducer = KafkaTestUtil.createMockProducer<String, String>()
    private val brukernotifikasjonbestillingRepository = BrukernotifikasjonbestillingRepository(database)
    private val varselForwarder = VarselForwarder(
        metricsCollector = metricsCollector,
        varselRapidProducer = VarselRapidProducer(rapidKafkaProducer, "rapid"),
        brukernotifikasjonbestillingRepository = brukernotifikasjonbestillingRepository
    )
    private val eventService = OppgaveInputEventService(varselForwarder = varselForwarder)

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

        runBlocking {
            inputEventConsumer.startPolling()
            KafkaTestUtil.delayUntilCommittedOffset(inputKafkaConsumer, KafkaTestTopics.oppgaveInputTopicName, oppgaveEvents.size.toLong())
            inputEventConsumer.stopPolling()
        }
    }

    @Test
    fun `Sender validerte oppgaver til intern-topic`() {
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
        oppgaveJson["smsVarslingstekst"].asText() shouldBe oppgaveAvroValue.getSmsVarslingstekst()
        oppgaveJson["epostVarslingstekst"].asText() shouldBe oppgaveAvroValue.getEpostVarslingstekst()
        oppgaveJson["epostVarslingstittel"].asText() shouldBe oppgaveAvroValue.getEpostVarslingstittel()
    }

    @Test
    fun `Lagrer bestillingene i basen`() {
        runBlocking {
            val brukernotifikasjonbestillinger = database.dbQuery { getAllBrukernotifikasjonbestilling() }
            brukernotifikasjonbestillinger.size shouldBe goodEvents.size

            val (oppgaveKey, _) = goodEvents.first()
            brukernotifikasjonbestillinger.first {
                it.eventId == oppgaveKey.getEventId()
            }.apply {
                eventtype shouldBe Eventtype.OPPGAVE
                fodselsnummer shouldBe oppgaveKey.getFodselsnummer()
            }
        }
    }
    private fun createEvents() = (1..10).map {
        createNokkelInputWithEventIdAndGroupId(
            eventId = UUID.randomUUID().toString(),
            groupId = it.toString()
        ) to oppgaveInput()
    }

    private fun createOppgaveWithNullFields() = listOf(
        createNokkelInputWithEventIdAndGroupId(
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
        createNokkelInputWithEventId( "notUuidOrUlid") to oppgaveInput()

    private fun createEventWithDuplicateId(existingEventId: String) =
        createNokkelInputWithEventId(existingEventId) to oppgaveInput()
}
