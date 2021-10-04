package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.internal.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.NokkelEventPairObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

internal class FeilresponsTransformerTest {

    @Test
    fun `Skal transformere alle duplikat til feilrespons`() {
        val duplicateEvents = listOf(
                NokkelEventPairObjectMother.createEventPair("eventId-1", "systembruker-1"),
                NokkelEventPairObjectMother.createEventPair("eventId-2", "systembruker-2")
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.BESKJED, duplicateEvents)

        feilrespons.size.`should be equal to`(duplicateEvents.size)
    }

    @Test
    fun `Skal transformere duplikat til feilrespons og returnere alle selvom nokkel er helt lik`() {
        val duplicateEventsWithExactlyTheSameKey = listOf(
                NokkelEventPairObjectMother.createEventPair("eventId", "systembruker"),
                NokkelEventPairObjectMother.createEventPair("eventId", "systembruker")
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.BESKJED, duplicateEventsWithExactlyTheSameKey)

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
