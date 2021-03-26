package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.feilrespons

import no.nav.brukernotifikasjon.schemas.internal.Feilrespons
import no.nav.brukernotifikasjon.schemas.internal.NokkelFeilrespons
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneOffset

object FeilresponsTransformer {

    fun createFeilrespons(eventId: String, systembruker: String, exception: Exception, type: Eventtype): RecordKeyValueWrapper<NokkelFeilrespons, Feilrespons> {
        val nokkelFeilrespons = toNokkelFeilrespons(eventId, systembruker, type)
        val feilrespons = toFeilrespons(exception)
        return RecordKeyValueWrapper(nokkelFeilrespons, feilrespons)
    }

    fun toNokkelFeilrespons(eventId: String, systembruker: String, type: Eventtype): NokkelFeilrespons {
        return NokkelFeilrespons.newBuilder()
                .setSystembruker(systembruker)
                .setEventId(eventId)
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