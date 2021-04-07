package no.nav.personbruker.brukernotifikasjonbestiller.database

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.createBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.deleteAllBrukernotifikasjonbestilling

suspend fun createBrukernotifikasjonbestillinger(database: H2Database, eventer: List<Brukernotifikasjonbestilling>) {
    database.dbQuery {
        createBrukernotifikasjonbestilling(eventer)
    }
}

suspend fun deleteAllBrukernotifikasjonbestillinger(database: H2Database) {
    database.dbQuery {
        deleteAllBrukernotifikasjonbestilling()
    }
}