package no.nav.personbruker.brukernotifikasjonbestiller.common.database

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.createBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.deleteAllBrukernotifikasjonbestilling

suspend fun H2Database.createBrukernotifikasjonbestillinger(eventer: List<Brukernotifikasjonbestilling>) {
    dbQuery {
        createBrukernotifikasjonbestilling(eventer)
    }
}

suspend fun H2Database.deleteAllBrukernotifikasjonbestillinger() {
    dbQuery {
        deleteAllBrukernotifikasjonbestilling()
    }
}