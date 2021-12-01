package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventMetricsSessionLegacy(val eventtype: Eventtype) {

    private val log: Logger = LoggerFactory.getLogger(EventMetricsSessionLegacy::class.java)

    private val countProcessedEventsBySysUser = HashMap<String, Int>()
    private val countFailedEventsBySysUser = HashMap<String, Int>()
    private val countDuplicateKeyBySysUser = HashMap<String, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countSuccessfulEventForProducer(producer: String) {
        countProcessedEventsBySysUser[producer] = countProcessedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEventForProducer(producer: String) {
        countFailedEventsBySysUser[producer] = countFailedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateEvents(duplicateEvents: List<Pair<NokkelIntern, *>>) {
        duplicateEvents.map {
            it.first
        }.forEach { duplicateEvent ->
            countDuplicateEventForProducer(duplicateEvent.getSystembruker())
        }
    }

    fun countDuplicateEventForProducer(producer: String) {
        countDuplicateKeyBySysUser[producer] = countDuplicateKeyBySysUser.getOrDefault(producer, 0).inc()
    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getEventsSeen(producer: String): Int {
        return getEventsProcessed(producer) + getEventsFailed(producer)
    }

    fun getEventsProcessed(producer: String): Int {
        return countProcessedEventsBySysUser.getOrDefault(producer, 0)
    }

    fun getEventsFailed(producer: String): Int {
        return countFailedEventsBySysUser.getOrDefault(producer, 0)
    }

    fun getDuplicateKeys(producer: String): Int {
        return countDuplicateKeyBySysUser.getOrDefault(producer, 0)
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

    fun getUniqueProducer(): List<String> {
        val producers = ArrayList<String>()
        producers.addAll(countProcessedEventsBySysUser.keys)
        producers.addAll(countFailedEventsBySysUser.keys)
        return producers.distinct()
    }
}
