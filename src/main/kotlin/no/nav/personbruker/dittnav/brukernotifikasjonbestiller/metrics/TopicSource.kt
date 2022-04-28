package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

enum class TopicSource {
    AIVEN;

    override fun toString(): String {
        return name
    }
}
