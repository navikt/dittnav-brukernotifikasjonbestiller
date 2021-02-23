package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID

object DoneTransformer {

    fun toDoneInternal(externalDone: Done): DoneIntern {
        return DoneIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(externalDone.getTidspunkt())
                .setGrupperingsId(externalDone.getGrupperingsId())
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalDone: Done): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(externalNokkel.getEventId())
                .setSystembruker(externalNokkel.getSystembruker())
                .setFodselsnummer(externalDone.getFodselsnummer())
                .build()
    }
}
