package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsTransformer {

    fun toNokkelFeilrespons(externalNokkel: Nokkel, type: Eventtype): NokkelFeilrespons {
        return NokkelFeilrespons.newBuilder()
                .setSystembruker(externalNokkel.getSystembruker())
                .setEventId(externalNokkel.getEventId())
                .setBrukernotifikasjonstype(type.toString())
                .build()
    }

    fun toFeilrespons(exception: Exception): Feilrespons {
        return Feilrespons.newBuilder()
                .setTidspunkt(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .setFeilmelding(exception.message)
                .build()
    }

    fun toNonNullNokkel(): Nokkel {
        return Nokkel.newBuilder()
                .setSystembruker("empty")
                .setEventId("empty")
                .build()
    }
}