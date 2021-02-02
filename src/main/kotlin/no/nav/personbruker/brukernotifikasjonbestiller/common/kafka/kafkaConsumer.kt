package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer

fun <K, V> KafkaConsumer<K, V>.rollbackToLastCommitted() {
    val assignedPartitions = assignment()
    val partitionCommittedInfo = committed(assignedPartitions)
    partitionCommittedInfo.forEach { (partition, lastCommitted) ->
        seek(partition, lastCommitted.offset())
    }
}
