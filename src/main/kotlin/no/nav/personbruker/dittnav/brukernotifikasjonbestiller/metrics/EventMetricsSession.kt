package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.brukernotifikasjonbestilling.Brukernotifikasjonbestilling
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventMetricsSession(val eventtype: Eventtype) {

    private val log: Logger = LoggerFactory.getLogger(EventMetricsSession::class.java)

    private val countProcessedEventsBySysUser = HashMap<String, Int>()
    private val countFailedEventsBySysUser = HashMap<String, Int>()
    private val countDuplicateKeyBySysUser = HashMap<String, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countSuccessfulEventForSystemUser(systemUser: String) {
        countProcessedEventsBySysUser[systemUser] = countProcessedEventsBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEventForSystemUser(systemUser: String) {
        countFailedEventsBySysUser[systemUser] = countFailedEventsBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun countDuplicateEvents(duplicateEvents: List<Brukernotifikasjonbestilling>) {
        duplicateEvents.forEach { duplicateEvent ->
            countDuplicateEventForSystemUser(duplicateEvent.systembruker)
            log.info("${duplicateEvent.eventtype} med eventId: ${duplicateEvent.eventId} og systembruker: ${duplicateEvent.eventId} er et duplikat. Legger derfor ikke eventet på topic igjen.")
        }
    }

    fun countDuplicateEventForSystemUser(systemUser: String) {
        countDuplicateKeyBySysUser[systemUser] = countDuplicateKeyBySysUser.getOrDefault(systemUser, 0).inc()
    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getEventsSeen(systemUser: String): Int {
        return getEventsProcessed(systemUser) + getEventsFailed(systemUser)
    }

    fun getEventsProcessed(systemUser: String): Int {
        return countProcessedEventsBySysUser.getOrDefault(systemUser, 0)
    }

    fun getEventsFailed(systemUser: String): Int {
        return countFailedEventsBySysUser.getOrDefault(systemUser, 0)
    }

    fun getDuplicateKeys(systemUser: String): Int {
        return countDuplicateKeyBySysUser.getOrDefault(systemUser, 0)
    }

    fun getEventsSeen(): Int {
        return getEventsProcessed() + getEventsFailed() + getNokkelWasNull()
    }

    fun getEventsProcessed(): Int {
        return countProcessedEventsBySysUser.values.sum()
    }

    fun getEventsFailed(): Int {
        return countFailedEventsBySysUser.values.sum()
    }

    fun getDuplicateKeys(): HashMap<String, Int> {
        return countDuplicateKeyBySysUser
    }

    fun getNokkelWasNull(): Int {
        return countNokkelWasNull
    }

    fun getUniqueSystemUser(): List<String> {
        val systemUsers = ArrayList<String>()
        systemUsers.addAll(countProcessedEventsBySysUser.keys)
        systemUsers.addAll(countFailedEventsBySysUser.keys)
        return systemUsers.distinct()
    }
}