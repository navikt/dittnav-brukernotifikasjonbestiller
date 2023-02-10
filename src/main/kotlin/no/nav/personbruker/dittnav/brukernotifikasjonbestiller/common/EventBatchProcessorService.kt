package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<K, V> {

    suspend fun processEvents(events: ConsumerRecords<K, V>)
}
