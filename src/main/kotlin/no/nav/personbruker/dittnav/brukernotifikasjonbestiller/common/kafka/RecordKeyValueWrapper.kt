package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

data class RecordKeyValueWrapper <K, V> (
        val key: K,
        val value: V
)
