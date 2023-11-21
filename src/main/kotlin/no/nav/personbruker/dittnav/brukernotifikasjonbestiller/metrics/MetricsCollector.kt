package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype

class MetricsCollector {

    suspend fun recordMetrics(eventType: Eventtype, block: suspend EventMetricsSession.() -> Unit) {
        val session = EventMetricsSession(eventType)
        block.invoke(session)

        if (session.getEventsSeen() > 0) {
            handleSeenEvents(session)
            handleProcessedEvents(session)
            handleFailedEvents(session)
        }
    }

    private fun handleSeenEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberSeen = session.getEventsSeen(producer)
            val eventTypeName = session.eventtype.toString()

            PrometheusMetricsCollector.registerEventsSeen(numberSeen, eventTypeName, producer.appName)
        }
    }

    private fun handleProcessedEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberProcessed = session.getEventsProcessed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberProcessed > 0) {
                PrometheusMetricsCollector.registerEventsProcessed(numberProcessed, eventTypeName, producer.appName)
            }
        }
    }

    private fun handleFailedEvents(session: EventMetricsSession) {
        session.getUniqueProducer().forEach { producer ->
            val numberFailed = session.getEventsFailed(producer)
            val eventTypeName = session.eventtype.toString()

            if (numberFailed > 0) {
                PrometheusMetricsCollector.registerEventsFailed(numberFailed, eventTypeName, producer.appName)
            }
        }
    }
}
