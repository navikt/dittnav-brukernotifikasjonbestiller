package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsTransformer {

    fun createFeilrespons(externalNokkel: Nokkel, exception: Exception, type: Eventtype): RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = toNokkelFeilrespons(externalNokkel, type)
        val feilrespons = toFeilrespons(exception)
        return RecordKeyValueWrapper(nokkelFeilrespons, feilrespons)
    }

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
                .setFeilmelding(exception.toString())
                .build()
    }

}