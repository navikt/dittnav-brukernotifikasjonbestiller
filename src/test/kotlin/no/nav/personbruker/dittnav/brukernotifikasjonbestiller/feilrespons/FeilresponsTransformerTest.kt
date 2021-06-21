package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.BrukernotifikasjonbestillingObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

internal class FeilresponsTransformerTest {

    @Test
    fun `should transform from external to feilrespons`() {
        val eventtype = Eventtype.BESKJED

        val nokkelExternal = AvroNokkelObjectMother.createNokkelWithEventId("eventId")
        val exception = FieldValidationException("Simulert feil i test.")

        val nokkelFeilrespons = FeilresponsTransformer.toNokkelFeilrespons(nokkelExternal.getEventId(), nokkelExternal.getSystembruker(), eventtype)
        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)

        nokkelFeilrespons.getSystembruker() `should be equal to` nokkelExternal.getSystembruker()
        nokkelFeilrespons.getEventId() `should be equal to` nokkelExternal.getEventId()
        nokkelFeilrespons.getBrukernotifikasjonstype() `should be equal to` eventtype.toString()
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.VALIDERINGSFEIL.toString()
    }

    @Test
    fun `Skal transformere alle duplikat til feilrespons`() {
        val duplicateEvents = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId-1", "systembruker-1", Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId-2", "systembruker-2", Eventtype.BESKJED)
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(duplicateEvents)

        feilrespons.size.`should be equal to`(duplicateEvents.size)
    }

    @Test
    fun `Skal transformere duplikat til feilrespons og returnere alle selvom nokkel er helt lik`() {
        val duplicateEventsWithExactlyTheSameKey = listOf(
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED),
                BrukernotifikasjonbestillingObjectMother.createBrukernotifikasjonbestilling("eventId", "systembruker", Eventtype.BESKJED)
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(duplicateEventsWithExactlyTheSameKey)

        feilrespons.size.`should be equal to`(duplicateEventsWithExactlyTheSameKey.size)
    }

    @Test
    fun `Skal sette duplikat som begrunnelse ved DuplicateEventException`() {
        val exception = DuplicateEventException("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.DUPLIKAT.toString()
    }

    @Test
    fun `Skal sette valideringsfeil som begrunnelse ved FieldValidationException`() {
        val exception = FieldValidationException("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.VALIDERINGSFEIL.toString()
    }

    @Test
    fun `Skal sette annet som begrunnelse ved generell exception`() {
        val exception = Exception("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() `should be equal to` exception.toString()
        feilrespons.getBegrunnelse() `should be equal to` FeilresponsBegrunnelse.UKJENT.toString()
    }
}
