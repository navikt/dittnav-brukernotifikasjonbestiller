package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.createBrukernotifikasjonbestilling

suspend fun LocalPostgresDatabase.createBrukernotifikasjonbestillinger(eventer: List<Brukernotifikasjonbestilling>) {
    dbQuery {
        createBrukernotifikasjonbestilling(eventer)
    }
}