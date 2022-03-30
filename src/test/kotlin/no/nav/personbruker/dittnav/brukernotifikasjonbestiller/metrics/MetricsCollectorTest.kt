package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetricsCollectorTest {

    private val metricsReporter = mockk<MetricsReporter>(relaxed = true)
    private val metricsCollector = MetricsCollector(metricsReporter)

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
        mockkObject(PrometheusMetricsCollector)
    }

    @Test
    internal fun `should register events to prometheus`() {
        val producer = NamespaceAppName("ns", "app")
        val eventType = Eventtype.OPPGAVE
        runBlocking {
            metricsCollector.recordMetrics(eventType) {
                countSuccessfulEventForProducer(producer)
                countFailedEventForProducer(producer)
            }
        }

        verify { PrometheusMetricsCollector.registerEventsSeen(2, eventType.toString(), producer.appName) }
        verify { PrometheusMetricsCollector.registerEventsProcessed(1, eventType.toString(), producer.appName) }
        verify { PrometheusMetricsCollector.registerEventsFailed(1, eventType.toString(), producer.appName) }

        confirmVerified(PrometheusMetricsCollector)
    }
}
