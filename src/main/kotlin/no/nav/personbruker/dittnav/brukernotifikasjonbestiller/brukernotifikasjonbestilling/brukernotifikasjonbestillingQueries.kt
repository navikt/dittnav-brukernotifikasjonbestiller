package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.*
import java.sql.Array
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

private val createQuery = """INSERT INTO brukernotifikasjonbestilling (eventId, systembruker, eventtype, prosesserttidspunkt) VALUES (?, ?, ?, ?)"""

fun Connection.createBrukernotifikasjonbestilling(events: List<Brukernotifikasjonbestilling>): ListPersistActionResult<Brukernotifikasjonbestilling> =
        executeBatchPersistQuery(createQuery) {
            events.forEach { event ->
                buildStatementForSingleRow(event)
                addBatch()
            }
        }.toBatchPersistResult(events)


fun <T> Connection.getEventsByEventId(events: MutableMap<NokkelIntern, T>): List<Brukernotifikasjonbestilling> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventId= ANY(?) """)
                .use {
                    it.setArray(1, toVarcharArray(events))
                    it.executeQuery().mapList { toBrukernotifikasjonbestilling() }
                }

fun ResultSet.toBrukernotifikasjonbestilling(): Brukernotifikasjonbestilling {
    return Brukernotifikasjonbestilling(
            eventId = getString("eventId"),
            systembruker = getString("systembruker"),
            eventtype = getString("eventtype"),
            prosesserttidspunkt = getUtcDateTime("prosesserttidspunkt")
    )
}

private fun <T> Connection.toVarcharArray(events: MutableMap<NokkelIntern, T>): Array {
    val eventIds = events.keys.toList().map { it.getEventId() }
    return createArrayOf("VARCHAR", eventIds.toTypedArray())
}

private fun PreparedStatement.buildStatementForSingleRow(event: Brukernotifikasjonbestilling) {
    setString(1, event.eventId)
    setString(2, event.systembruker)
    setString(3, event.eventtype)
    setObject(4, event.prosesserttidspunkt)
}
