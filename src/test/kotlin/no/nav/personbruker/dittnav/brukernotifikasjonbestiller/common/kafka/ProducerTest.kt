package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.RetriableKafkaException
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.exception.UnretriableKafkaException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.KafkaException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class ProducerTest {
    private val topicName = "topic"
    private val kafkaProducer: KafkaProducer<String, String> = mockk()

    private val producer = Producer(topicName, kafkaProducer)

    private val records = listOf(
            RecordKeyValueWrapper("key1", "record1"),
            RecordKeyValueWrapper("key2", "record2"),
            RecordKeyValueWrapper("key3", "record3")
    )

    @AfterEach
    fun cleanUp() {
        clearMocks(kafkaProducer)
    }

    @Test
    fun `Should start and commit transaction if sending events and performing codependent action is successful`() {
        every { kafkaProducer.send(any()) } returns null
        every { kafkaProducer.beginTransaction() } returns Unit
        every { kafkaProducer.commitTransaction() } returns Unit

        runBlocking {
            producer.sendEventsTransactionally(records)
        }

        verify(exactly = records.size) { kafkaProducer.send(any()) }
        verify(exactly = 1) { kafkaProducer.beginTransaction() }
        verify(exactly = 1) { kafkaProducer.commitTransaction() }
    }

    @Test
    fun `Should abort transaction if periodic error occurred when sending event to kafka`() {
        every { kafkaProducer.send(any()) } throws KafkaException()
        every { kafkaProducer.beginTransaction() } returns Unit
        every { kafkaProducer.abortTransaction() } returns Unit

        shouldThrow<RetriableKafkaException> {
            runBlocking {
                producer.sendEventsTransactionally(records)
            }
        }


        verify(exactly = 1) { kafkaProducer.beginTransaction() }
        verify(exactly = 1) { kafkaProducer.abortTransaction() }
    }

    @Test
    fun `Should close producer if unknown error occurred`() {
        every { kafkaProducer.send(any()) } throws Exception()
        every { kafkaProducer.beginTransaction() } returns Unit
        every { kafkaProducer.close() } returns Unit

        shouldThrow<UnretriableKafkaException> {
            runBlocking {
                producer.sendEventsTransactionally(records)
            }
        }

        verify(exactly = 1) { kafkaProducer.beginTransaction() }
        verify(exactly = 1) { kafkaProducer.close() }
    }
}
