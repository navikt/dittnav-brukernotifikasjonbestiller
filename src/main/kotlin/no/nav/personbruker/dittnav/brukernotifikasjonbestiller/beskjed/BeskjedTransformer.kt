package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validatePrefererteKanaler

object BeskjedTransformer {

    fun toBeskjedInternal(externalBeskjed: Beskjed): BeskjedIntern {
        return BeskjedIntern.newBuilder()
                .setUlid(createULID())
                .setTidspunkt(externalBeskjed.getTidspunkt())
                .setSynligFremTil(externalBeskjed.getSynligFremTil())
                .setGrupperingsId(validateNonNullFieldMaxLength(externalBeskjed.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setTekst(validateNonNullFieldMaxLength(externalBeskjed.getTekst(), "tekst", MAX_LENGTH_TEXT_BESKJED))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalBeskjed.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.BESKJED)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalBeskjed.getSikkerhetsnivaa()))
                .setEksternVarsling(externalBeskjed.getEksternVarsling())
                .setPrefererteKanaler(validatePrefererteKanaler(externalBeskjed.getEksternVarsling(), externalBeskjed.getPrefererteKanaler()))
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalBeskjed: Beskjed): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalBeskjed.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .build()
    }
}
