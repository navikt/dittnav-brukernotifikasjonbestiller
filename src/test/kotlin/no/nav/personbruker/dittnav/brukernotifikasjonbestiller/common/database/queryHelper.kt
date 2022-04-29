package no.nav.personbruker.brukernotifikasjonbestiller.common.database

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.createBrukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.deleteAllBrukernotifikasjonbestilling

suspend fun LocalPostgresDatabase.createBrukernotifikasjonbestillinger(eventer: List<Brukernotifikasjonbestilling>) {
    dbQuery {
        createBrukernotifikasjonbestilling(eventer)
    }
}

suspend fun LocalPostgresDatabase.deleteAllBrukernotifikasjonbestillinger() {
    dbQuery {
        deleteAllBrukernotifikasjonbestilling()
    }
}
