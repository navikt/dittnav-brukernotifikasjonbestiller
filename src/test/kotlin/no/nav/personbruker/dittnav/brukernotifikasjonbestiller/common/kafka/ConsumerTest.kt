package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.legacy.BeskjedLegacy
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.exception.UnvalidatableRecordException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother.ConsumerRecordsObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.serviceuser.ServiceUserMappingException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.Status
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.DisconnectException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class ConsumerTest {

    private val kafkaConsumer = mockk<KafkaConsumer<NokkelLegacy, BeskjedLegacy>>(relaxUnitFun = true)
    private val eventBatchProcessorService = mockk<EventBatchProcessorService<NokkelLegacy, BeskjedLegacy>>(relaxed = true)

    @BeforeEach
    fun clearMocks() {
        io.mockk.clearMocks(kafkaConsumer, eventBatchProcessorService)
    }

    @Test
    fun `Skal commit-e mot Kafka hvis ingen feil skjer`() {
        val topic = "dummyTopicNoErrors"
        every { kafkaConsumer.poll(any<Duration>()) } returns ConsumerRecordsObjectMother.giveMeANumberOfBeskjedLegacyRecords(1, topic)

        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(300)

            consumer.status().status `should be equal to` Status.OK
            consumer.stopPolling()
        }
        verify(atLeast = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal ikke kvittere ut eventer som lest, hvis en ukjent feil skjer`() {
        val topic = "dummyTopicUkjentFeil"
        every { kafkaConsumer.poll(any<Duration>()) } returns ConsumerRecordsObjectMother.giveMeANumberOfBeskjedLegacyRecords(1, topic)
        coEvery { eventBatchProcessorService.processEvents(any()) } throws Exception("Simulert feil i en test")

        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(10)
            consumer.job.join()
            consumer.status().status `should be equal to` Status.ERROR
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal ikke kvittere ut eventer som lest, hvis transformering av et eller flere eventer feiler`() {
        val topic = "dummyTopicUntransformable"
        every { kafkaConsumer.poll(any<Duration>()) } returns ConsumerRecordsObjectMother.giveMeANumberOfBeskjedLegacyRecords(1, topic)
        coEvery { eventBatchProcessorService.processEvents(any()) } throws UnvalidatableRecordException("Simulert feil i en test")

        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(10)
            consumer.job.join()
            consumer.status().status `should be equal to` Status.ERROR
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal fortsette pollingen hvis det er en retriable exception throw by Kafka`() {
        val topic = "dummyTopicKafkaRetriable"
        val retriableKafkaException = DisconnectException("Simulert feil i en test")
        every { kafkaConsumer.poll(any<Duration>()) } throws retriableKafkaException
        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            `Vent litt for aa bevise at det fortsettes aa polle`()

            consumer.status().status `should be equal to` Status.OK
            consumer.stopPolling()
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }


    @Test
    fun `Skal ikke commit-e mot kafka hvis det IKKE har blitt funnet noen event-er`() {
        val topic = "dummyTopicNoRecordsFound"
        every { kafkaConsumer.poll(any<Duration>()) } returns ConsumerRecordsObjectMother.giveMeANumberOfBeskjedLegacyRecords(0, topic)

        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(30)

            consumer.status().status `should be equal to` Status.OK
            consumer.stopPolling()
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal ikke commit-e mot kafka hvis det har skjedd en CancellationException, som skjer ved stopping av polling`() {
        val topic = "dummyTopicCancellationException"
        val cancellationException = CancellationException("Simulert feil i en test")
        every { kafkaConsumer.poll(any<Duration>()) } throws cancellationException
        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(10)
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `skal stoppe polling etter en ServiceUserMappingException`() {
        val topic = "dummyTopicServiceUserMappingException"
        every { kafkaConsumer.poll(any<Duration>()) } throws ServiceUserMappingException("")
        val consumer: Consumer<NokkelLegacy, BeskjedLegacy> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        val jobActive = runBlocking {
            consumer.startPolling()
            consumer.`vent til inaktiv eller avbryt`(1000)
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
        jobActive `should be equal to` false
    }

    private suspend fun `Vent litt for aa bevise at det fortsettes aa polle`() {
        delay(10)
    }

    private suspend fun <K, T> Consumer<K, T>.`vent til inaktiv eller avbryt`(maxDelay: Int = 1000): Boolean {
        for (i in 0..maxDelay step 10) {
            delay(10)

            if (!job.isActive) {
                return false
            }
        }
        return job.isActive
    }

}
