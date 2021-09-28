package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.legacy.NokkelLegacy
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.RecordKeyValueWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords

interface EventBatchProcessorService<K, V> {

    suspend fun processEvents(events: ConsumerRecords<K, V>)

    val ConsumerRecord<NokkelLegacy, V>.systembruker: String? get() = key().getSystembruker()

    fun ConsumerRecords<K, V>.asWrapperList() : List<RecordKeyValueWrapper<K, V>> = map { record ->
        RecordKeyValueWrapper(record.key(), record.value())
    }
}
