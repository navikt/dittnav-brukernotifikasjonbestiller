package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import no.nav.personbruker.dittnav.common.util.kafka.RecordKeyValueWrapper
import no.nav.personbruker.dittnav.common.util.kafka.producer.KafkaProducerWrapper

class Producer<K, V>(private val kafkaProducer: KafkaProducerWrapper<K, V>) {

    fun produceEvents(events: List<RecordKeyValueWrapper<K, V>>) {
        kafkaProducer.sendEventsTransactionally(events)
    }

    fun flushAndClose() {
        kafkaProducer.flushAndClose()
    }
}
