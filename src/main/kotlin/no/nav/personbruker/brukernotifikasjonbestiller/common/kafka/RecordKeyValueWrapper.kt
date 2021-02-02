package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.Nokkel

data class RecordKeyValueWrapper <T> (
        val key: Nokkel,
        val value: T
)
