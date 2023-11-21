package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tms.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.LocalDateTimeHelper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.toLocalDateTime
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.apache.avro.generic.GenericRecord
import java.time.LocalDateTime

data class Varsel(
    @JsonIgnore
    val type: Eventtype,
    val systembruker: String,
    val namespace: String,
    val appnavn: String,
    val eventId: String,
    val eventTidspunkt: LocalDateTime,
    val forstBehandlet: LocalDateTime,
    val fodselsnummer: String,
    val grupperingsId: String,
    val tekst: String,
    val link: String,
    val sikkerhetsnivaa: Int,
    val synligFremTil: LocalDateTime?,
    val aktiv: Boolean,
    val eksternVarsling: Boolean,
    val prefererteKanaler: List<String>,
    val smsVarslingstekst: String?,
    val epostVarslingstekst: String?,
    val epostVarslingstittel: String?
)

fun Pair<NokkelInput, GenericRecord>.toVarsel(type: Eventtype): Varsel {
    return Varsel(
        type = type,
        systembruker = "N/A",
        namespace = first.getNamespace(),
        appnavn = first.getAppnavn(),
        eventId = first.getEventId(),
        eventTidspunkt = (second.get("tidspunkt") as Long).toLocalDateTime(),
        forstBehandlet = LocalDateTimeHelper.nowAtUtc(),
        fodselsnummer = first.getFodselsnummer(),
        grupperingsId = first.getGrupperingsId(),
        tekst = second.get("tekst") as String,
        link = second.get("link") as String,
        sikkerhetsnivaa = second.get("sikkerhetsnivaa") as Int,
        synligFremTil = (if(second.hasField("synligFremTil")) second.get("synligFremTil") as Long? else null)?.toLocalDateTime(),
        aktiv = true,
        eksternVarsling = second.get("eksternVarsling") as Boolean,
        prefererteKanaler = (second.get("prefererteKanaler") as List<*>).map { it as String },
        smsVarslingstekst = second.get("smsVarslingstekst") as String?,
        epostVarslingstekst = second.get("epostVarslingstekst") as String?,
        epostVarslingstittel = second.get("epostVarslingstittel") as String?
    )
}
