package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

enum class Eventtype(val eventtype: String) {
    BESKJED("beskjed"),
    OPPGAVE("oppgave"),
    STATUSOPPDATERING("statusoppdatering"),
    DONE("done"),
    FEILRESPONS("feilrespons"),
    BESKJEDINTERN("beskjedintern")
}

