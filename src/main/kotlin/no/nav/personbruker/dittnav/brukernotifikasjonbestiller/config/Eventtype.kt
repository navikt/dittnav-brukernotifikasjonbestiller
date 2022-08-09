package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

enum class Eventtype(val eventtype: String) {
    BESKJED("beskjed"),
    OPPGAVE("oppgave"),
    INNBOKS("innboks"),
    DONE("done"),
    FEILRESPONS("feilrespons"),
    BESKJEDINTERN("beskjedintern"),
    OPPGAVEINTERN("oppgaveintern"),
    INNBOKSINTERN("innboksintern"),
    DONEINTERN("doneintern"),
    BESKJED_LEGACY("beskjedLegacy"),
    OPPGAVE_LEGACY("oppgaveLegacy"),
    INNBOKS_LEGACY("innboksLegacy"),
    DONE_LEGACY("doneLegacy"),
}

