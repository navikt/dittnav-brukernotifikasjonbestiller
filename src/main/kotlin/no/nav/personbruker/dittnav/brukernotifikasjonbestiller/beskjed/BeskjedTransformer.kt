package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.internal.BeskjedIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object BeskjedTransformer {

    fun toBeskjedInternal(externalNokkel: Nokkel, externalBeskjed: Beskjed): BeskjedIntern {
        return BeskjedIntern()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalBeskjed: Beskjed): NokkelIntern {
        return NokkelIntern()
    }
}
