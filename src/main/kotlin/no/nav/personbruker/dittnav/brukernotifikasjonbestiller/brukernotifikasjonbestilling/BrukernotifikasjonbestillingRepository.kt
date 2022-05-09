package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.BrukernotifikasjonKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DoneKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.time.LocalDateTime
import java.time.ZoneId

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
                            prosesserttidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                            fodselsnummer = event.first.getFodselsnummer()
                    )
            )
        }
        return result
    }
}
