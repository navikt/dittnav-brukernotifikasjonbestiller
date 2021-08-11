package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern

data class DuplicateCheckResult<T> (
    val validEvents: List<Pair<NokkelIntern, T>>,
    val duplicateEvents: List<Pair<NokkelIntern, T>>
)
