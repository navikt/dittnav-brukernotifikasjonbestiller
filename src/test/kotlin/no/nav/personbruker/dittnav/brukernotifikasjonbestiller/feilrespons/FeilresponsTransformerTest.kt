package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

internal class FeilresponsTransformerTest {

    private val eventId = "1"

    @Test
    fun `should transform from external to feilrespons`() {
        val eventtype = Eventtype.BESKJED

        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId(eventId)
        val exception = FieldValidationException("Simulert feil i test.")

        val nokkelFeilrespons = FeilresponsTransformer.toNokkelFeilrespons(nokkelExternal, eventtype)
        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)

        nokkelFeilrespons.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        nokkelFeilrespons.getEventId() `should be equal to` nokkelExternal.getEventId()
        nokkelFeilrespons.getBrukernotifikasjonstype() `should be equal to` eventtype.toString()
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
    }
}