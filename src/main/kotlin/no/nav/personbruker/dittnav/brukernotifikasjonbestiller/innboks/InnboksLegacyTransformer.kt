package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.innboks

import no.nav.brukernotifikasjon.schemas.legacy.InnboksLegacy
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.InnboksIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler


class InnboksLegacyTransformer(private val mapper: ServiceUserMapper) {

    fun toInnboksInternal(externalInnboks: InnboksLegacy): InnboksIntern {
        return InnboksIntern.newBuilder()
                .setTidspunkt(externalInnboks.getTidspunkt())
                .setTekst(validateNonNullFieldMaxLength(externalInnboks.getTekst(), "tekst", MAX_LENGTH_TEXT_INNBOKS))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalInnboks.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.INNBOKS)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalInnboks.getSikkerhetsnivaa()))
                .setEksternVarsling(externalInnboks.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalInnboks.getEksternVarsling(), externalInnboks.getPrefererteKanaler()))
                .build()
    }

    fun toNokkelInternal(externalNokkel: NokkelLegacy, externalInnboks: InnboksLegacy): NokkelIntern {
        val origin = mapper.getNamespaceAppName(externalNokkel.getSystembruker())

        return NokkelIntern.newBuilder()
                .setUlid(createULID())
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalInnboks.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalInnboks.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .setNamespace(origin.namespace)
                .setAppnavn(origin.appName)
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .build()
    }
}
