package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class BrukernotifikasjonbestillingRepository(private val database: Database) {

    suspend fun <T> fetchEventsThatMatchEventId(events: List<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> {
        var resultat = emptyList<Brukernotifikasjonbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getEventsByEventId(events)
        }

        return resultat
    }

    suspend fun fetchDuplicatesOfEventtype(eventtype: Eventtype, isDuplicateEvents: List<Brukernotifikasjonbestilling>): List<Brukernotifikasjonbestilling> {
        val result = mutableListOf<Brukernotifikasjonbestilling>()
        database.queryWithExceptionTranslation {
            isDuplicateEvents.forEach { event ->
                val duplicates = getEventsByIds(event.eventId, event.systembruker, eventtype)
                if (duplicates.isNotEmpty()) {
                    result.add(duplicates.first())
                }
            }
        }
        return result.distinct()
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
                            prosesserttidspunkt = java.time.LocalDateTime.now()
                    )
            )
        }
        return result
    }
}