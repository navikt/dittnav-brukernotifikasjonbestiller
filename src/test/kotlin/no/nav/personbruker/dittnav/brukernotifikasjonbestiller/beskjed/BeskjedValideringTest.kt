package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import de.huxhorn.sulky.ulid.ULID
import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeskjedValideringTest {

    private val validation = Validation()

    @Test
    fun `nøkkel kan ikke være null`() {
        val externalEvents = ConsumerRecordsObjectMother.createInputConsumerRecords(null, AvroBeskjedInputObjectMother.createBeskjedInput())

        validation.validate(externalEvents.first()) shouldBe false
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

        validation.validate(externalEvents.first()) shouldBe false
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}