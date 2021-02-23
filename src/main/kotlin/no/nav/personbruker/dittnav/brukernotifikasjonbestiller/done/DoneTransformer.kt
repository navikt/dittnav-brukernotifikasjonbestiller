package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done

import no.nav.brukernotifikasjon.schemas.Done
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.internal.DoneIntern
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createRandomStringULID
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.validation.validateDateTime

object DoneTransformer {

    fun toDoneInternal(externalDone: Done): DoneIntern {
        return DoneIntern.newBuilder()
                .setUlid(createRandomStringULID())
                .setTidspunkt(validateDateTime(externalDone.getTidspunkt(), "tidspunkt"))
                .setGrupperingsId(validateNonNullFieldMaxLength(externalDone.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
                .build()
    }

    fun toNokkelInternal(externalNokkel: Nokkel, externalDone: Done): NokkelIntern {
        return NokkelIntern.newBuilder()
                .setEventId(validateNonNullFieldMaxLength(externalNokkel.getEventId(), "eventId", MAX_LENGTH_EVENTID))
                .setSystembruker(validateNonNullFieldMaxLength(externalNokkel.getSystembruker(), "systembruker", MAX_LENGTH_SYSTEMBRUKER))
                .setFodselsnummer(validateNonNullFieldMaxLength(externalDone.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
                .build()
    }
}
