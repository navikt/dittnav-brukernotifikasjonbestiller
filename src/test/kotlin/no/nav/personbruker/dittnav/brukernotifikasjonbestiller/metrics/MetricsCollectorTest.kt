package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics


import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.prometheus.PrometheusMetricsCollector
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetricsCollectorTest {

    private val metricsReporter = mockk<MetricsReporter>()
    private val producerNameResolver = mockk<ProducerNameResolver>()
    val producerName = "x-dittnav"
    val producerAlias = "dittnav"

    @BeforeEach
    fun cleanup() {
        clearAllMocks()
        mockkObject(PrometheusMetricsCollector)
    }

    @Test
    fun `should replace system name with alias for event processed`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val producerNameForPrometheus = slot<String>()
        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit
        coEvery { PrometheusMetricsCollector.registerSeenEvents(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED) {
                countSuccessfulEventForSystemUser(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEvents(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerProcessedEvents(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should replace system name with alias for event failed`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val capturedTags = slot<Map<String, String>>()
        val producerNameForPrometheus = slot<String>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit
        every { PrometheusMetricsCollector.registerFailedEvents(any(), any(), capture(producerNameForPrometheus)) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED) {
                countFailedEventForSystemUser(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEvents(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerFailedEvents(any(), any(), any()) }

        producerNameForPrometheus.captured `should be equal to` producerAlias
        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should report correct number of events`() {
        coEvery { producerNameResolver.getProducerNameAlias(any()) } returns "test-user"

        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollector(metricsReporter, nameScrubber)

        val capturedFieldsForSeen = slot<Map<String, Any>>()
        val capturedFieldsForProcessed = slot<Map<String, Any>>()
        val capturedFieldsForFailed = slot<Map<String, Any>>()
        val capturedNokkelWasNull = slot<Map<String, Any>>()

        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_SEEN, capture(capturedFieldsForSeen), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSED, capture(capturedFieldsForProcessed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_FAILED, capture(capturedFieldsForFailed), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_NOKKEL_NULL, capture(capturedNokkelWasNull), any()) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED) {
                countSuccessfulEventForSystemUser("producer")
                countSuccessfulEventForSystemUser("producer")
                countFailedEventForSystemUser("producer")
                countNokkelWasNull()
            }
        }

        coVerify(exactly = 5) { metricsReporter.registerDataPoint(any(), any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerSeenEvents(3, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerProcessedEvents(2, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerFailedEvents(1, any(), any()) }
        verify(exactly = 1) { PrometheusMetricsCollector.registerNokkelWasNullEvents(1, any(), any()) }

        capturedFieldsForSeen.captured["counter"] `should be equal to` 3
        capturedFieldsForProcessed.captured["counter"] `should be equal to` 2
        capturedFieldsForFailed.captured["counter"] `should be equal to` 1
        capturedNokkelWasNull.captured["counter"] `should be equal to` 1
    }

}