package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.statusoppdatering

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Statusoppdatering
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.brukernotifikasjon.schemas.internal.StatusoppdateringIntern

object StatusoppdateringTransformer {

    fun toStatusoppdateringInternal(externalNokkel: Nokkel, externalStatusoppdatering: Statusoppdatering): StatusoppdateringIntern {
        return StatusoppdateringIntern()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalStatusoppdatering: Statusoppdatering): NokkelIntern {
        return NokkelIntern()
    }
}
