package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.EventBatchProcessorService
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.DisconnectException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class ConsumerTest {

    private val kafkaConsumer = mockk<KafkaConsumer<NokkelInput, BeskjedInput>>(relaxed = true)
    private val eventBatchProcessorService = mockk<EventBatchProcessorService<NokkelInput, BeskjedInput>>(relaxed = true)

    @BeforeEach
    fun clearMocks() {
        io.mockk.clearMocks(kafkaConsumer, eventBatchProcessorService)
    }

    @Test
    fun `Skal commit-e mot Kafka hvis ingen feil skjer`() {
        val topic = "dummyTopicNoErrors"
        every { kafkaConsumer.poll(any<Duration>()) } returns singleBeskjedRecord()

        val consumer: Consumer<NokkelInput, BeskjedInput> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(300)

            consumer.stopPolling()
        }
        verify(atLeast = 1) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal ikke kvittere ut eventer som lest, hvis en ukjent feil skjer`() {
        val topic = "dummyTopicUkjentFeil"
        every { kafkaConsumer.poll(any<Duration>()) } returns singleBeskjedRecord()
        coEvery { eventBatchProcessorService.processEvents(any()) } throws Exception("Simulert feil i en test")

        val consumer: Consumer<NokkelInput, BeskjedInput> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(10)
            consumer.job.join()
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal fortsette pollingen hvis det er en retriable exception throw by Kafka`() {
        val topic = "dummyTopicKafkaRetriable"
        val retriableKafkaException = DisconnectException("Simulert feil i en test")
        every { kafkaConsumer.poll(any<Duration>()) } throws retriableKafkaException
        val consumer: Consumer<NokkelInput, BeskjedInput> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            `Vent litt for aa bevise at det fortsettes aa polle`()

            consumer.stopPolling()
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }


    @Test
    fun `Skal ikke commit-e mot kafka hvis det IKKE har blitt funnet noen event-er`() {
        val topic = "dummyTopicNoRecordsFound"
        every { kafkaConsumer.poll(any<Duration>()) } returns ConsumerRecords(mapOf())

        val consumer: Consumer<NokkelInput, BeskjedInput> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(30)

            consumer.stopPolling()
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    @Test
    fun `Skal ikke commit-e mot kafka hvis det har skjedd en CancellationException, som skjer ved stopping av polling`() {
        val topic = "dummyTopicCancellationException"
        val cancellationException = CancellationException("Simulert feil i en test")
        every { kafkaConsumer.poll(any<Duration>()) } throws cancellationException
        val consumer: Consumer<NokkelInput, BeskjedInput> = Consumer(topic, kafkaConsumer, eventBatchProcessorService)

        runBlocking {
            consumer.startPolling()
            delay(10)
        }
        verify(exactly = 0) { kafkaConsumer.commitSync() }
    }

    private suspend fun `Vent litt for aa bevise at det fortsettes aa polle`() {
        delay(10)
    }

    private fun singleBeskjedRecord() =
        ConsumerRecords(
            mapOf(TopicPartition("topic", 0) to listOf(
                ConsumerRecord(
                    "topic",
                    0,
                    0,
                    NokkelTestData.nokkel(),
                    BeskjedTestData.beskjedInput()
                ))
            )
        )


}
