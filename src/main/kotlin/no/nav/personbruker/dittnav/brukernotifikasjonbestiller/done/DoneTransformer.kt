package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

object DoneTransformer {

    fun toDoneInternal(externalNokkel: Nokkel, externalDone: Done): DoneIntern {
        return DoneIntern()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalDone: Done): NokkelIntern {
        return NokkelIntern()
    }
}
