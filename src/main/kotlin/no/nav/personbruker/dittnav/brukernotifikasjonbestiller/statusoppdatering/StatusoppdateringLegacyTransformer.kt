package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.brukernotifikasjon.schemas.legacy.StatusoppdateringLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMapper

class StatusoppdateringLegacyTransformer(private val mapper: ServiceUserMapper) {

    fun toStatusoppdateringInternal(externalStatusoppdatering: StatusoppdateringLegacy): StatusoppdateringIntern {
        return StatusoppdateringIntern.newBuilder()
                .setTidspunkt(externalStatusoppdatering.getTidspunkt())
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalStatusoppdatering.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.STATUSOPPDATERING)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalStatusoppdatering.getSikkerhetsnivaa()))
                .setStatusGlobal(validateStatusGlobal(externalStatusoppdatering.getStatusGlobal()))
                .setStatusIntern(externalStatusoppdatering.getStatusIntern()?.let { status -> validateMaxLength(status, "statusIntern", MAX_LENGTH_STATUSINTERN)})
                .setSakstema(validateNonNullFieldMaxLength(externalStatusoppdatering.getSakstema(), "sakstema", MAX_LENGTH_SAKSTEMA))
                .build()
    }

    fun toNokkelInternal(externalNokkel: NokkelLegacy, externalStatusoppdatering: StatusoppdateringLegacy): NokkelIntern {
        val origin = mapper.getNamespaceAppName(externalNokkel.getSystembruker())

        return NokkelIntern.newBuilder()
                .setUlid(createULID())
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalStatusoppdatering.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalStatusoppdatering.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .setNamespace(origin.namespace)
                .setAppnavn(origin.appName)
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .build()
    }
}
