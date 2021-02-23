package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID

object StatusoppdateringTransformer {

    fun toStatusoppdateringInternal(externalStatusoppdatering: Statusoppdatering): StatusoppdateringIntern {
        return StatusoppdateringIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(externalStatusoppdatering.getTidspunkt())
                .setGrupperingsId(externalStatusoppdatering.getGrupperingsId())
                .setLink(externalStatusoppdatering.getLink())
                .setSikkerhetsnivaa(externalStatusoppdatering.getSikkerhetsnivaa())
                .setStatusGlobal(externalStatusoppdatering.getStatusGlobal())
                .setStatusIntern(externalStatusoppdatering.getStatusIntern())
                .setSakstema(externalStatusoppdatering.getSakstema())
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalStatusoppdatering: Statusoppdatering): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(externalNokkel.getEventId())
                .setSystembruker(externalNokkel.getSystembruker())
                .setFodselsnummer(externalStatusoppdatering.getFodselsnummer())
                .build()
    }
}
