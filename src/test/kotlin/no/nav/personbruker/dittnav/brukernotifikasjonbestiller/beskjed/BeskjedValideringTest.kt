package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import io.kotest.matchers.shouldBe
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeskjedValideringTest {

    @Test
    @Disabled
    fun `Alle felter må bli validert før videresending`() {
        val externalNokkel = NokkelTestData.nokkel()
        val externalBeskjed = AvroBeskjedInputObjectMother.createBeskjedInput(
            //link = "",
            //sikkerhetsnivaa = 5
            //eksternVarsling = true,
            //prefererteKanaler = listOf("SMS")
            //epostVarslingstekst = ""
            smsVarslingstekst = ""
        )

        val externalEvents = ConsumerRecordsObjectMother.createInputConsumerRecords(
            externalNokkel,
            externalBeskjed)

        val validation = BeskjedValidation(externalEvents.first().value())
        validation.isValid() shouldBe false
        validation.failedValidators.map { it.javaClass } shouldBe listOf(
            HasTekst::class.java,
        )
    }

    @Test
    @Disabled
    fun `eventtype må være beskjed`() {

    }
}