package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.TopicPartition

object KafkaTestUtil {

    suspend fun <K, V> delayUntilTotalOffset(
        consumer: MockConsumer<K, V>,
        topics: List<String>,
        offset: Long
    ) {
        val partitions = topics.map { TopicPartition(it, 0) }.toSet()
        withTimeout(10000) {
            while (consumer.committed(partitions).values.sumOf { it.offset() } < offset) {
                delay(10)
            }
        }
    }

    fun <K, V> createMockConsumer(topics: List<String>): MockConsumer<K, V> {
        val partitions = topics.map { TopicPartition(it, 0) }
        return MockConsumer<K, V>(OffsetResetStrategy.EARLIEST).also {
            it.subscribe(topics)
            it.rebalance(partitions)
            it.updateBeginningOffsets(partitions.associateWith { 0 })
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
