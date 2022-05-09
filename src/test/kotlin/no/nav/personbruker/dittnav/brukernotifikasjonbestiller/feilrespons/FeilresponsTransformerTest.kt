package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import io.kotest.matchers.shouldBe
import no.nav.brukernotifikasjon.schemas.builders.exception.FieldValidationException
import no.nav.brukernotifikasjon.schemas.output.domain.FeilresponsBegrunnelse
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.DuplicateEventException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.NokkelEventPairObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.junit.jupiter.api.Test

internal class FeilresponsTransformerTest {

    @Test
    fun `Skal transformere alle duplikat til feilrespons`() {
        val duplicateEvents = listOf(
                NokkelEventPairObjectMother.createEventPair("eventId-1", "systembruker-1"),
                NokkelEventPairObjectMother.createEventPair("eventId-2", "systembruker-2")
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.BESKJED, duplicateEvents)

        feilrespons.size shouldBe duplicateEvents.size
    }

    @Test
    fun `Skal transformere duplikat til feilrespons og returnere alle selvom nokkel er helt lik`() {
        val duplicateEventsWithExactlyTheSameKey = listOf(
                NokkelEventPairObjectMother.createEventPair("eventId", "systembruker"),
                NokkelEventPairObjectMother.createEventPair("eventId", "systembruker")
        )
        val feilrespons = FeilresponsTransformer.createFeilresponsFromDuplicateEvents(Eventtype.BESKJED, duplicateEventsWithExactlyTheSameKey)

        feilrespons.size shouldBe duplicateEventsWithExactlyTheSameKey.size
    }

    @Test
    fun `Skal sette duplikat som begrunnelse ved DuplicateEventException`() {
        val exception = DuplicateEventException("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() shouldBe exception.toString()
        feilrespons.getBegrunnelse() shouldBe FeilresponsBegrunnelse.DUPLIKAT.toString()
    }

    @Test
    fun `Skal sette valideringsfeil som begrunnelse ved FieldValidationException`() {
        val exception = FieldValidationException("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() shouldBe exception.toString()
        feilrespons.getBegrunnelse() shouldBe FeilresponsBegrunnelse.VALIDERINGSFEIL.toString()
    }

    @Test
    fun `Skal sette annet som begrunnelse ved generell exception`() {
        val exception = Exception("Simulert feil i test.")

        val feilrespons = FeilresponsTransformer.toFeilrespons(exception)
        feilrespons.getFeilmelding() shouldBe exception.toString()
        feilrespons.getBegrunnelse() shouldBe FeilresponsBegrunnelse.UKJENT.toString()
    }
}
