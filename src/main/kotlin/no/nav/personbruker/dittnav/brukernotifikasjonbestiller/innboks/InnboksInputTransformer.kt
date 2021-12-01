package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.input.InnboksInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelInputTransformer

object InnboksInputTransformer {

    fun toInternal(nokkelExternal: NokkelInput, innboksExternal: InnboksInput): Pair<NokkelIntern, InnboksIntern> {

        return NokkelInputTransformer.toNokkelInternal(nokkelExternal) to toInnboksInternal(innboksExternal)
    }

    private fun toInnboksInternal(externalInnboks: InnboksInput): InnboksIntern {
        return InnboksIntern.newBuilder()
                .setTidspunkt(externalInnboks.getTidspunkt())
                .setTekst(validateNonNullFieldMaxLength(externalInnboks.getTekst(), "tekst", MAX_LENGTH_TEXT_INNBOKS))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalInnboks.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.INNBOKS)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalInnboks.getSikkerhetsnivaa()))
                .setEksternVarsling(externalInnboks.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalInnboks.getEksternVarsling(), externalInnboks.getPrefererteKanaler()))
                .build()
    }
}
