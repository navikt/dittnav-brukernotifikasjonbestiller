package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel

import no.nav.brukernotifikasjon.schemas.builders.util.ValidationUtil.*
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.createULID

object NokkelInputTransformer {

    fun toNokkelInternal(externalNokkel: NokkelInput): NokkelIntern {

        return NokkelIntern.newBuilder()
            .setUlid(createULID())
            .setEventId(validateEventId(externalNokkel.getEventId()))
            .setGrupperingsId(validateNonNullFieldMaxLength(externalNokkel.getGrupperingsId(), "grupperingsId", MAX_LENGTH_GRUPPERINGSID))
            .setFodselsnummer(validateNonNullFieldMaxLength(externalNokkel.getFodselsnummer(), "fodselsnummer", MAX_LENGTH_FODSELSNUMMER))
            .setNamespace(externalNokkel.getNamespace())
            .setAppnavn(externalNokkel.getAppnavn())
            .setSystembruker("N/A")
            .build()
    }
}
