package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.KafkaTestUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeskjedValideringTest {

    private val kafkaProducerMock = KafkaTestUtil.createMockProducer<String, String>()

    private val beskjedEventService = BeskjedInputEventService(
        metricsCollector =  mockk(relaxed = true),
        handleDuplicateEvents = mockk(),
        eventDispatcher = mockk(),
        beskjedRapidProducer = BeskjedRapidProducer(kafkaProducerMock, "rapid")
    )

    @Test
    fun `nøkkel kan ikke være null`() {
        val externalEvents = ConsumerRecordsObjectMother.createInputConsumerRecords(null, AvroBeskjedInputObjectMother.createBeskjedInput())

        runBlocking {
            beskjedEventService.processEvents2(externalEvents)
        }

        kafkaProducerMock.history().size shouldBe 0
    }

    @Test
    fun `Alle felter må bli validert før videresending`() {
        val externalNokkel = AvroNokkelInputObjectMother.createNokkelInputWithEventId(ULID().nextULID())
        val externalBeskjed = AvroBeskjedInputObjectMother.createBeskjedInput(
            //link = "",
            //sikkerhetsnivaa = 5
            //eksternVarsling = true,
            //prefererteKanaler = listOf("SMS")
            //epostVarslingstekst = ""
            smsVarslingstekst = ""
        )

        val externalEvents = ConsumerRecordsObjectMother.createInputConsumerRecords(externalNokkel, externalBeskjed)

        runBlocking {
            beskjedEventService.processEvents2(externalEvents)
        }

        kafkaProducerMock.history().size shouldBe 0
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}