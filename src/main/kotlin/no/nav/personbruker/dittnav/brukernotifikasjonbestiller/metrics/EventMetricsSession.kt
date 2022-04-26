package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.brukernotifikasjon.schemas.internal.NokkelIntern
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EventMetricsSession(val eventtype: Eventtype) {

    private val log: Logger = LoggerFactory.getLogger(EventMetricsSession::class.java)

    private val countProcessedEventsBySysUser = HashMap<NamespaceAppName, Int>()
    private val countFailedEventsBySysUser = HashMap<NamespaceAppName, Int>()
    private val countDuplicateKeyBySysUser = HashMap<NamespaceAppName, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countSuccessfulEventForProducer(producer: NamespaceAppName) {
        countProcessedEventsBySysUser[producer] = countProcessedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEventForProducer(producer: NamespaceAppName) {
        countFailedEventsBySysUser[producer] = countFailedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateEvents(duplicateEvents: List<Pair<NokkelIntern, *>>) {
        duplicateEvents.map {
            it.first
        }.forEach { duplicateEvent ->
            countDuplicateEventForProducer(duplicateEvent.getProducerNamespaceAppName())
        }
    }

    fun countDuplicateEventForProducer(producer: NamespaceAppName) {
        countDuplicateKeyBySysUser[producer] = countDuplicateKeyBySysUser.getOrDefault(producer, 0).inc()
    }

    fun timeElapsedSinceSessionStartNanos(): Long {
        return System.nanoTime() - startTime
    }

    fun getEventsSeen(producer: NamespaceAppName): Int {
        return getEventsProcessed(producer) + getEventsFailed(producer)
    }

    fun getEventsProcessed(producer: NamespaceAppName): Int {
        return countProcessedEventsBySysUser.getOrDefault(producer, 0)
    }

    fun getEventsFailed(producer: NamespaceAppName): Int {
        return countFailedEventsBySysUser.getOrDefault(producer, 0)
    }

    fun getDuplicateKeys(producer: NamespaceAppName): Int {
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

    fun getNokkelWasNull(): Int {
        return countNokkelWasNull
    }

    fun getUniqueProducer(): List<NamespaceAppName> {
        val producers = countProcessedEventsBySysUser.keys + countFailedEventsBySysUser.keys
        return producers.distinct()
    }

    companion object {
        fun NokkelIntern.getProducerNamespaceAppName() = NamespaceAppName(getNamespace(), getAppnavn())
    }
}
