package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.Beskjed
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.ListPersistActionResult
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.Varsel
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

    suspend fun persist(beskjeder: List<Beskjed>) {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(toBrukernotifikasjonbestilling(beskjeder))
        }
    }

    suspend fun persistVarsler(varsler: List<Varsel>) {
        return database.queryWithExceptionTranslation {
            createBrukernotifikasjonbestilling(varsler.map { it.toBrukernotifikasjonbestilling() })
        }
    }

    private fun toBrukernotifikasjonbestilling(beskjeder: List<Beskjed>): List<Brukernotifikasjonbestilling> {
        return beskjeder.map { beskjed ->
                Brukernotifikasjonbestilling(
                    eventId = beskjed.eventId,
                    systembruker = beskjed.systembruker,
                    eventtype = Eventtype.BESKJED,
                    prosesserttidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
                    fodselsnummer = beskjed.fodselsnummer
                )

        }
    }

    private fun Varsel.toBrukernotifikasjonbestilling() =
        Brukernotifikasjonbestilling(
            eventId = eventId,
            systembruker = systembruker,
            eventtype = Eventtype.BESKJED,
            prosesserttidspunkt = LocalDateTime.now(ZoneId.of("UTC")),
            fodselsnummer = fodselsnummer
        )



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
