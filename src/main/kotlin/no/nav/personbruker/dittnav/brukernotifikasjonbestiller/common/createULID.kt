package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import de.huxhorn.sulky.ulid.ULID

fun createULID(): String {
    return ULID().nextULID()
}