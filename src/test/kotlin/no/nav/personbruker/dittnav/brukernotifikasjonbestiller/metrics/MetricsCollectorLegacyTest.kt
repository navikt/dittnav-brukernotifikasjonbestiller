package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics


import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.NamespaceAppName
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Eventtype
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.metrics.influx.*
import no.nav.personbruker.dittnav.common.metrics.MetricsReporter
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MetricsCollectorLegacyTest {

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
        val metricsCollector = MetricsCollectorLegacy(metricsReporter, nameScrubber)

        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED) {
                countSuccessfulEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }

        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should replace system name with alias for event failed`() {

        coEvery { producerNameResolver.getProducerNameAlias(producerName) } returns producerAlias
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollectorLegacy(metricsReporter, nameScrubber)

        val capturedTags = slot<Map<String, String>>()

        coEvery { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), capture(capturedTags)) } returns Unit
        coEvery { metricsReporter.registerDataPoint(KAFKA_EVENTS_PROCESSING_TIME, any(), any()) } returns Unit

        runBlocking {
            metricsCollector.recordMetrics(Eventtype.BESKJED) {
                countFailedEventForProducer(producerName)
            }
        }

        coVerify(exactly = 2) { metricsReporter.registerDataPoint(not(KAFKA_EVENTS_PROCESSING_TIME), any(), any()) }

        capturedTags.captured["producer"] `should be equal to` producerAlias
    }

    @Test
    fun `should report correct number of events`() {
        coEvery { producerNameResolver.getProducerNameAlias(any()) } returns "test-user"

        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollectorLegacy(metricsReporter, nameScrubber)

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
                countSuccessfulEventForProducer("producer")
                countSuccessfulEventForProducer("producer")
                countFailedEventForProducer("producer")
                countNokkelWasNull()
            }
        }

        coVerify(exactly = 5) { metricsReporter.registerDataPoint(any(), any(), any()) }

        capturedFieldsForSeen.captured["counter"] `should be equal to` 3
        capturedFieldsForProcessed.captured["counter"] `should be equal to` 2
        capturedFieldsForFailed.captured["counter"] `should be equal to` 1
        capturedNokkelWasNull.captured["counter"] `should be equal to` 1
    }

    @Test
    internal fun `should register events to prometheus`() {
        val producer = "producer"
        val eventType = Eventtype.OPPGAVE
        coEvery { producerNameResolver.getProducerNameAlias(any()) } returns producer
        coEvery { metricsReporter.registerDataPoint(any(), any(), any()) } returns Unit
        val nameScrubber = ProducerNameScrubber(producerNameResolver)
        val metricsCollector = MetricsCollectorLegacy(metricsReporter, nameScrubber)

        runBlocking {
            metricsCollector.recordMetrics(eventType) {
                countSuccessfulEventForProducer(producer)
                countFailedEventForProducer(producer)
            }
        }

        verify { PrometheusMetricsCollector.registerEventsSeen(2, eventType.toString(), producer) }
        verify { PrometheusMetricsCollector.registerEventsProcessed(1, eventType.toString(), producer) }
        verify { PrometheusMetricsCollector.registerEventsFailed(1, eventType.toString(), producer) }

        confirmVerified(PrometheusMetricsCollector)
    }

}
