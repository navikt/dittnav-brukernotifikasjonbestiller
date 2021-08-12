package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.BrukernotifikasjonKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DoneKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class BrukernotifikasjonbestillingRepository(private val database: Database) {

    suspend fun fetchBrukernotifikasjonKeysThatMatchEventIds(eventIds: List<String>): List<BrukernotifikasjonKey> {
        return database.queryWithExceptionTranslation {
            getEventKeysByEventIds(eventIds)
        }
    }

    suspend fun fetchDoneKeysThatMatchEventIds(eventIds: List<String>): List<DoneKey> {
        return database.queryWithExceptionTranslation {
            getDoneKeysByEventIds(eventIds)
        }
    }

    suspend fun <T> persistInOneBatch(entities: List<Pair<NokkelIntern, T>>, eventtype: Eventtype): ListPersistActionResult<Brukernotifikasjonbestilling> {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(toBrukernotifikasjonbestilling(entities, eventtype))
        }
    }

    private fun <T> toBrukernotifikasjonbestilling(events: List<Pair<NokkelIntern, T>>, eventtype: Eventtype): List<Brukernotifikasjonbestilling> {
        val result = mutableListOf<Brukernotifikasjonbestilling>()
        events.forEach { event ->
            result.add(
                    Brukernotifikasjonbestilling(
                            eventId = event.first.getEventId(),
                            systembruker = event.first.getSystembruker(),
                            eventtype = eventtype,
                            prosesserttidspunkt = java.time.LocalDateTime.now(),
                            fodselsnummer = event.first.getFodselsnummer()
                    )
            )
        }
        return result
    }
}
