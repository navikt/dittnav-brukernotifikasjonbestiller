package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler

class BeskjedLegacyTransformer(private val mapper: ServiceUserMapper) {

    fun toBeskjedInternal(externalBeskjed: Beskjed): BeskjedIntern {
        return BeskjedIntern.newBuilder()
                .setTidspunkt(externalBeskjed.getTidspunkt())
                .setSynligFremTil(externalBeskjed.getSynligFremTil())
                .setTekst(validateNonNullFieldMaxLength(externalBeskjed.getTekst(), "tekst", MAX_LENGTH_TEXT_BESKJED))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalBeskjed.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.BESKJED)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalBeskjed.getSikkerhetsnivaa()))
                .setEksternVarsling(externalBeskjed.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalBeskjed.getEksternVarsling(), externalBeskjed.getPrefererteKanaler()))
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalBeskjed: Beskjed): NokkelIntern {
        val origin = mapper.getNamespaceAppName(externalNokkel.getSystembruker())

        return NokkelIntern.newBuilder()
                .setUlid(createULID())
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalBeskjed.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalBeskjed.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .setNamespace(origin.namespace)
                .setAppnavn(origin.appName)
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .build()
    }
}
