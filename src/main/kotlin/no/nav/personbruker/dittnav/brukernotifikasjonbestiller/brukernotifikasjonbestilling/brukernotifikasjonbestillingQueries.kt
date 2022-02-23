package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.BrukernotifikasjonKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.DoneKey
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import java.sql.Array
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

private val createQuery = """INSERT INTO brukernotifikasjonbestilling (eventId, systembruker, eventtype, prosesserttidspunkt, fodselsnummer) VALUES (?, ?, ?, ?, ?)"""

fun Connection.createBrukernotifikasjonbestilling(events: List<Brukernotifikasjonbestilling>): ListPersistActionResult<Brukernotifikasjonbestilling> =
        executeBatchPersistQuery(createQuery) {
            events.forEach { event ->
                buildStatementForSingleRow(event)
                addBatch()
            }
        }.toBatchPersistResult(events)


fun <T> Connection.getEventsByEventId(events: List<Pair<NokkelIntern, T>>): List<Brukernotifikasjonbestilling> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) """)
                .use {
                    it.setArray(1, toVarcharArray(events))
                    it.executeQuery().mapList { toBrukernotifikasjonbestilling() }
                }

fun Connection.getDoneKeysByEventIds(eventIds: List<String>): List<DoneKey> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) """)
                .use {
                    it.setArray(1, createArrayOf("VARCHAR", eventIds.toTypedArray()))
                    it.executeQuery().mapList { toDoneKey() }
                }

fun Connection.getEventKeysByEventIds(eventIds: List<String>): List<BrukernotifikasjonKey> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) """)
                .use {
                    it.setArray(1, createArrayOf("VARCHAR", eventIds.toTypedArray()))
                    it.executeQuery().mapList { toBrukernotifikasjonKey() }
                }

fun Connection.getExistingEventIdsExcludingDone(eventIds: List<String>): List<String> =
        prepareStatement("""SELECT eventId FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) AND eventtype !=?""")
                .use {
                    it.setArray(1, createArrayOf("VARCHAR", eventIds.toTypedArray()))
                    it.setString(2, Eventtype.DONE.toString())
                    it.executeQuery().mapList { toEventIdString() }
                }

fun Connection.getExistingEventIdsForDone(eventIds: List<String>): List<String> =
        prepareStatement("""SELECT eventId FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) AND eventtype=?""")
                .use {
                    it.setArray(1, createArrayOf("VARCHAR", eventIds.toTypedArray()))
                    it.setString(2, Eventtype.DONE.toString())
                    it.executeQuery().mapList { toEventIdString() }
                }

fun Connection.getEventsByIds(eventId: String, systembruker: String, eventtype: Eventtype): List<Brukernotifikasjonbestilling> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventId=? AND systembruker=? AND eventtype=? """)
                .use {
                    it.setString(1, eventId)
                    it.setString(2, systembruker)
                    it.setString(3, eventtype.toString())
                    it.executeQuery().mapList { toBrukernotifikasjonbestilling() }
                }

fun ResultSet.toBrukernotifikasjonbestilling(): Brukernotifikasjonbestilling {
    return Brukernotifikasjonbestilling(
            eventId = getString("eventId"),
            systembruker = getString("systembruker"),
            eventtype = toEventtype(getString("eventtype")),
            prosesserttidspunkt = getUtcDateTime("prosesserttidspunkt"),
            fodselsnummer = getString("fodselsnummer")
    )
}

fun ResultSet.toBrukernotifikasjonKey(): BrukernotifikasjonKey {
    return BrukernotifikasjonKey(
            eventId = getString("eventId"),
            systembruker = getString("systembruker"),
            eventtype = toEventtype(getString("eventtype"))
    )
}

fun ResultSet.toEventIdString(): String {
    return getString("eventId")
}

fun ResultSet.toDoneKey(): DoneKey {
    return DoneKey(
            eventId = getString("eventId"),
            systembruker = getString("systembruker"),
            eventtype = toEventtype(getString("eventtype")),
            fodselsnummer = getString("fodselsnummer")
    )
}

private fun <T> Connection.toVarcharArray(events: List<Pair<NokkelIntern, T>>): Array {
    val eventIds = events.map { it.first.getEventId() }
    return createArrayOf("VARCHAR", eventIds.toTypedArray())
}

private fun PreparedStatement.buildStatementForSingleRow(event: Brukernotifikasjonbestilling) {
    setString(1, event.eventId)
    setString(2, event.systembruker)
    setString(3, event.eventtype.toString())
    setObject(4, event.prosesserttidspunkt)
    setString(5, event.fodselsnummer)
}
