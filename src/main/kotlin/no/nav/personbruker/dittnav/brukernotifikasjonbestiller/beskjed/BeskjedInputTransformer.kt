package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelInputTransformer

object BeskjedInputTransformer {

    fun toInternal(nokkelExternal: NokkelInput, beskjedExternal: BeskjedInput): Pair<NokkelIntern, BeskjedIntern> {

        return NokkelInputTransformer.toNokkelInternal(nokkelExternal) to toBeskjedInternal(beskjedExternal)
    }

    private fun toBeskjedInternal(externalBeskjed: BeskjedInput): BeskjedIntern {
        return BeskjedIntern.newBuilder()
                .setTidspunkt(externalBeskjed.getTidspunkt())
                .setSynligFremTil(externalBeskjed.getSynligFremTil())
                .setTekst(validateNonNullFieldMaxLength(externalBeskjed.getTekst(), "tekst", MAX_LENGTH_TEXT_BESKJED))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalBeskjed.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.BESKJED)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalBeskjed.getSikkerhetsnivaa()))
                .setEksternVarsling(externalBeskjed.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalBeskjed.getEksternVarsling(), externalBeskjed.getPrefererteKanaler()))
                .setEpostVarslingstekst(validateEpostVarslingstekst(externalBeskjed.getEksternVarsling(), externalBeskjed.getEpostVarslingstekst()))
                .setEpostVarslingstittel(validateEpostVarslingstittel(externalBeskjed.getEksternVarsling(), externalBeskjed.getEpostVarslingstittel()))
                .setSmsVarslingstekst(validateSmsVarslingstekst(externalBeskjed.getEksternVarsling(), externalBeskjed.getSmsVarslingstekst()))
                .build()
    }
}
