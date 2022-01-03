package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

enum class TopicSource {
    ON_PREM, AIVEN;

    override fun toString(): String {
        return name
    }
}
