package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.joda.time.DateTime
import java.sql.*
import java.sql.Array

fun <T> Connection.createBrukernotifikasjonbestilling(events: Map<NokkelIntern, T>, eventtype: Eventtype): ListPersistActionResult<NokkelIntern> =
        //TODO: konverter til brukernotifikasjonbestilling samtidig. Return: List<Brukernotifikasjonbestilling>. NokkelIntern != bnb
        executeBatchPersistQuery("""INSERT INTO brukernotifikasjonbestilling (eventid, systembruker, eventtype, eventtidspunkt) 
                                    |VALUES (?, ?, ?, ?, ?)""".trimMargin()) {
            events.forEach { event ->
                buildStatementForSingleRow(event.key, eventtype)
                addBatch()
            }
        }.toBatchPersistResult(events.keys.toList())


fun <T> Connection.getEventsByEventId(events: MutableMap<NokkelIntern, T>): List<Brukernotifikasjonbestilling> =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventid= ANY(?) """)
                .use {
                    it.setArray(1, toVarcharArray(events))
                    it.executeQuery().mapList { toBrukernotifikasjonbestilling() }
                }

fun Connection.getEventsByIds(eventid: String, systembruker: String, eventtype: Eventtype): Brukernotifikasjonbestilling =
        prepareStatement("""SELECT * FROM brukernotifikasjonbestilling WHERE eventid= ?, systembruker=?, eventtype=? """)
                .use {
                    it.setString(1, eventid)
                    it.setString(2, systembruker)
                    it.setString(3, eventtype.toString())
                    it.executeQuery().toBrukernotifikasjonbestilling()
                }


fun ResultSet.toBrukernotifikasjonbestilling(): Brukernotifikasjonbestilling {
    return Brukernotifikasjonbestilling(
            eventid = getString("eventid"),
            systembruker = getString("systembruker"),
            eventtype = getString("eventtype"),
            eventtidspunkt = getUtcDateTime("eventtidspunkt")
    )
}

private fun <T> Connection.toVarcharArray(events: MutableMap<NokkelIntern, T>): Array {
    val eventIds = events.keys.toList().map { it.getEventId() }
    return createArrayOf("VARCHAR", eventIds.toTypedArray())
}

private fun PreparedStatement.buildStatementForSingleRow(nokkelIntern: NokkelIntern, eventtype: Eventtype) {
    setString(1, nokkelIntern.getEventId())
    setString(2, nokkelIntern.getSystembruker())
    setString(3, eventtype.toString())
    setObject(4, DateTime.now(), Types.TIMESTAMP)
}
