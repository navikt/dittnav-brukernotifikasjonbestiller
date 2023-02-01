package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.oppgave

import java.time.LocalDateTime

data class Oppgave(
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