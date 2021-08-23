package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.Innboks
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID

object InnboksTransformer {

    fun toInnboksInternal(externalInnboks: Innboks): InnboksIntern {
        return InnboksIntern.newBuilder()
            .setUlid(createULID())
            .setTidspunkt(externalInnboks.getTidspunkt())
            .setGrupperingsId(validateNonNullFieldMaxLength(externalInnboks.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
            .setTekst(validateNonNullFieldMaxLength(externalInnboks.getTekst(), "tekst", MAX_LENGTH_TEXT_INNBOKS))
            .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalInnboks.getLink()), "link", ValidationUtil.MAX_LENGTH_LINK, ValidationUtil.isLinkRequired(Eventtype.INNBOKS)))
            .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalInnboks.getSikkerhetsnivaa()))
            .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalInnboks: Innboks): NokkelIntern {
        return NokkelIntern.newBuilder()
            .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
            .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
            .setFodselsnummer(validateNonNullFieldMaxLength(externalInnboks.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
            .build()
    }
}
