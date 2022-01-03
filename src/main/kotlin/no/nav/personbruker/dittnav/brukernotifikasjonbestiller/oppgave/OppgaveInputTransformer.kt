package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.OppgaveIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelInputTransformer

object OppgaveInputTransformer {

    fun toInternal(nokkelExternal: NokkelInput, oppgaveExternal: OppgaveInput): Pair<NokkelIntern, OppgaveIntern> {

        return NokkelInputTransformer.toNokkelInternal(nokkelExternal) to toOppgaveInternal(oppgaveExternal)
    }

    private fun toOppgaveInternal(externalOppgave: OppgaveInput): OppgaveIntern {
        return OppgaveIntern.newBuilder()
            .setTidspunkt(externalOppgave.getTidspunkt())
            .setSynligFremTil(externalOppgave.getSynligFremTil())
            .setTekst(validateNonNullFieldMaxLength(externalOppgave.getTekst(), "tekst", MAX_LENGTH_TEXT_OPPGAVE))
            .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalOppgave.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.OPPGAVE)))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalOppgave.getSikkerhetsnivaa()))
            .setEksternVarsling(externalOppgave.getEksternVarsling())
            .setPrefererteKanaler(validatePrefererteKanaler(externalOppgave.getEksternVarsling(), externalOppgave.getPrefererteKanaler()))
            .build()
    }
}
