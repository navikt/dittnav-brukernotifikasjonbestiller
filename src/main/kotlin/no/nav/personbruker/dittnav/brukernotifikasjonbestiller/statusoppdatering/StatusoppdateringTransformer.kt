package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.builders.domain.Eventtype
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validateDateTime

object StatusoppdateringTransformer {

    fun toStatusoppdateringInternal(externalStatusoppdatering: Statusoppdatering): StatusoppdateringIntern {
        return StatusoppdateringIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(validateDateTime(externalStatusoppdatering.getTidspunkt(), "tidspunkt"))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalStatusoppdatering.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .setLink(validateLinkAndConvertToString(validateLinkAndConvertToURL(externalStatusoppdatering.getLink()), "link", MAX_LENGTH_LINK, isLinkRequired(Eventtype.STATUSOPPDATERING)))
                .setSikkerhetsnivaa(validateSikkerhetsnivaa(externalStatusoppdatering.getSikkerhetsnivaa()))
                .setStatusGlobal(validateStatusGlobal(externalStatusoppdatering.getStatusGlobal()))
                .setStatusIntern(externalStatusoppdatering.getStatusIntern()?.let { status -> validateMaxLength(status, "statusIntern", MAX_LENGTH_STATUSINTERN)})
                .setSakstema(validateNonNullFieldMaxLength(externalStatusoppdatering.getSakstema(), "sakstema", MAX_LENGTH_SAKSTEMA))
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalStatusoppdatering: Statusoppdatering): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalStatusoppdatering.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .build()
    }
}
