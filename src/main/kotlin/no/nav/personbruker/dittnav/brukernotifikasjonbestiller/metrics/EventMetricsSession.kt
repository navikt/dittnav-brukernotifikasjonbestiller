package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.done.Done
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.Varsel

class EventMetricsSession(val eventtype: Eventtype) {

    private val countProcessedEventsBySysUser = HashMap<NamespaceAppName, Int>()
    private val countProcessedRapidEventsBySysUser = HashMap<NamespaceAppName, Int>()
    private val countFailedEventsBySysUser = HashMap<NamespaceAppName, Int>()
    private val countDuplicateKeyBySysUser = HashMap<NamespaceAppName, Int>()
    private var countNokkelWasNull: Int = 0
    private val startTime = System.nanoTime()

    fun countSuccessfulEventForProducer(producer: NamespaceAppName) {
        countProcessedEventsBySysUser[producer] = countProcessedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countSuccessfulRapidEventForProducer(namespace: String, appnavn: String) {
        val producer = NamespaceAppName(namespace = namespace, appName = appnavn)
        countProcessedRapidEventsBySysUser[producer] = countProcessedRapidEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countNokkelWasNull() {
        countNokkelWasNull++
    }

    fun countFailedEventForProducer(producer: NamespaceAppName) {
        countFailedEventsBySysUser[producer] = countFailedEventsBySysUser.getOrDefault(producer, 0).inc()
    }

    fun countDuplicateVarsler(duplicateVarsler: List<Varsel>) {
        duplicateVarsler.forEach {
            countDuplicateEventForProducer(NamespaceAppName(namespace = it.namespace, appName = it.appnavn))
        }
    }

    fun countDuplicateDone(duplicateDoneList: List<Done>) {
        duplicateDoneList.forEach {
            countDuplicateEventForProducer(NamespaceAppName(namespace = it.namespace, appName = it.appnavn))
        }
    }

    private fun countDuplicateEventForProducer(producer: NamespaceAppName) {
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

    fun getRapidEventsProcessed(producer: NamespaceAppName): Int {
        return countProcessedRapidEventsBySysUser.getOrDefault(producer, 0)
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
        val producers = countProcessedRapidEventsBySysUser.keys + countFailedEventsBySysUser.keys
        return producers.distinct()
    }
}
