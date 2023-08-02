package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition

object KafkaTestUtil {

    suspend fun <K, V> delayUntilCommittedOffset(
        consumer: MockConsumer<K, V>,
        topicName: String,
        offset: Long
    ) {
        val partition = TopicPartition(topicName, 0)
        withTimeout(10000) {
            while ((consumer.committed(setOf(partition))[partition]?.offset() ?: 0) < offset) {
                delay(10)
            }
        }
    }

    fun <K, V> createMockConsumer(topicName: String): MockConsumer<K, V> {
        val partition = TopicPartition(topicName, 0)
        return MockConsumer<K, V>(OffsetResetStrategy.EARLIEST).also {
            it.subscribe(listOf(partition.topic()))
            it.rebalance(listOf(partition))
            it.updateBeginningOffsets(mapOf(partition to 0))
        }
    }

    fun <K, V> createMockProducer(): MockProducer<K, V> {
        return MockProducer(
            true,
            { _: String, _: K -> ByteArray(0) }, //Dummy serializers
            { _: String, _: V -> ByteArray(0) }
        )
    }
}
