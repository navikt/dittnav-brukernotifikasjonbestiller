package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.LocalDateTimeHelper
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.Done
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.Varsel

class BrukernotifikasjonbestillingRepository(private val database: Database) {

    suspend fun fetchExistingEventIdsExcludingDone(eventIds: List<String>): List<String> {
        return database.queryWithExceptionTranslation {
            getExistingEventIdsExcludingDone(eventIds)
        }
    }

    suspend fun fetchExistingEventIdsForDone(eventIds: List<String>): List<String> {
        return database.queryWithExceptionTranslation {
            getExistingEventIdsForDone(eventIds)
        }
    }

    suspend fun persistVarsler(varsler: List<Varsel>) {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(varsler.map { it.toBrukernotifikasjonbestilling() })
        }
    }

    suspend fun persistDone(doneList: List<Done>) {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(doneList.map { it.toBrukernotifikasjonbestilling() })
        }
    }

    private fun Varsel.toBrukernotifikasjonbestilling() =
        Brukernotifikasjonbestilling(
            eventId = eventId,
            systembruker = systembruker,
            eventtype = type,
            prosesserttidspunkt = LocalDateTimeHelper.nowAtUtc(),
            fodselsnummer = fodselsnummer
        )

    private fun Done.toBrukernotifikasjonbestilling() =
        Brukernotifikasjonbestilling(
            eventId = eventId,
            systembruker = "N/A",
            eventtype = Eventtype.DONE,
            prosesserttidspunkt = LocalDateTimeHelper.nowAtUtc(),
            fodselsnummer = fodselsnummer
        )
}
