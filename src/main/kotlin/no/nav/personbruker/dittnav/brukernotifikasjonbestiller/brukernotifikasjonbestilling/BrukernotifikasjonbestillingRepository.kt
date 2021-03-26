package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class BrukernotifikasjonbestillingRepository(private val database: Database) {

    suspend fun <T> fetchEventsFromEventId(brukernotifikasjonbestillinger: MutableMap<NokkelIntern, T>): List<Brukernotifikasjonbestilling> {
        var resultat = emptyList<Brukernotifikasjonbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getEventsByEventId(brukernotifikasjonbestillinger)
        }

        return resultat
    }

    suspend fun fetchEventsIfDuplicate(isDuplicateEvents: List<Brukernotifikasjonbestilling>, eventtype: Eventtype): List<Brukernotifikasjonbestilling> {
        val resultat = mutableListOf<Brukernotifikasjonbestilling>()
        database.queryWithExceptionTranslation {
            isDuplicateEvents.forEach { event ->
                resultat.add(getEventsByIds(event.eventid, event.systembruker, eventtype))
            }
        }
        return resultat
    }

    suspend fun <T> persistInOneBatch(entities: Map<NokkelIntern, T>, eventtype: Eventtype): ListPersistActionResult<NokkelIntern> {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(entities, eventtype)
        }
    }
}