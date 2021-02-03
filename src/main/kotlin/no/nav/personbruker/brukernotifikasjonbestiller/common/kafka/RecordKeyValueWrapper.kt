package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

data class RecordKeyValueWrapper <K, V> (
        val key: K,
        val value: V
)
