package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class BrukernotifikasjonbestillingRepository(private val database: Database) {

    suspend fun <T> fetchEventsThatMatchEventId(events: MutableMap<NokkelIntern, T>): List<Brukernotifikasjonbestilling> {
        var resultat = emptyList<Brukernotifikasjonbestilling>()
        database.queryWithExceptionTranslation {
            resultat = getEventsByEventId(events)
        }

        return resultat
    }

    suspend fun <T> persistInOneBatch(entities: Map<NokkelIntern, T>, eventtype: Eventtype): ListPersistActionResult<Brukernotifikasjonbestilling> {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(toBrukernotifikasjonbestilling(entities, eventtype))
        }
    }

    private fun <T> toBrukernotifikasjonbestilling(events: Map<NokkelIntern, T>, eventtype: Eventtype): List<Brukernotifikasjonbestilling> {
        val result = mutableListOf<Brukernotifikasjonbestilling>()
        events.forEach { nokkel, event ->
            result.add(
                    Brukernotifikasjonbestilling(
                            eventId = nokkel.getEventId(),
                            systembruker = nokkel.getSystembruker(),
                            eventtype = eventtype.toString(),
                            prosesserttidspunkt = java.time.LocalDateTime.now()
                    )
            )
        }
        return result
    }
}