package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.databind.JsonNode
import no.nav.tms.varsel.action.Sensitivitet
import java.time.ZonedDateTime

fun JsonNode.asEpoch(): Long =
    ZonedDateTime.parse(asText()).toInstant().toEpochMilli()



fun Int.toSensitivitetString() = when (this) {
    3 -> Sensitivitet.Substantial.name.lowercase()
    4 -> Sensitivitet.High.name.lowercase()
    else -> throw IllegalArgumentException("$this er ikke et gyldig sikkerhetsnivå")
}
