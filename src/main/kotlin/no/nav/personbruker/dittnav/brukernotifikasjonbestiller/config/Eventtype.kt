package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

enum class Eventtype(val eventtype: String) {
    BESKJED("beskjed"),
    OPPGAVE("oppgave"),
    INNBOKS("innboks"),
    STATUSOPPDATERING("statusoppdatering"),
    DONE("done"),
    FEILRESPONS("feilrespons"),
    BESKJEDINTERN("beskjedintern"),
    OPPGAVEINTERN("oppgaveintern"),
    INNBOKSINTERN("innboksintern"),
    STATUSOPPDATERINGINTERN("statusoppdateringintern"),
    DONEINTERN("doneintern")
}

