package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.mapList
import java.sql.Connection

fun Connection.deleteAllBrukernotifikasjonbestilling() =
        prepareStatement("""DELETE FROM brukernotifikasjonbestilling""")
                .use { it.execute() }

fun Connection.getAllBrukernotifikasjonbestilling() =
        prepareStatement("""SELECT brukernotifikasjonbestilling.* FROM brukernotifikasjonbestilling""")
                .use {
                    it.executeQuery().mapList { toBrukernotifikasjonbestilling() }
                }