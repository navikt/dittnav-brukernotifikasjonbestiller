package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedLegacyObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelLegacyObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    private val defaultPartition = 1
    private val defaultOffset = 1

    fun giveMeANumberOfBeskjedLegacyRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<Nokkel, Beskjed> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Beskjed>>>()
        val recordsForSingleTopic = createBeskjedLegacyRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedLegacyRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<Nokkel, Beskjed>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedLegacyObjectMother.createBeskjedLegacy(i)
            val nokkel = AvroNokkelLegacyObjectMother.createNokkelLegacyWithEventId(i.toString())

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    fun <T> createLegacyConsumerRecords(nokkel: Nokkel?, event: T?, topic: String): ConsumerRecords<Nokkel, T> {
        val allRecords = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, T>>>()
        val recordsForSingleTopic = mutableListOf<ConsumerRecord<Nokkel, T>>()

        recordsForSingleTopic.add(ConsumerRecord(topic, defaultPartition, defaultOffset.toLong(), nokkel, event))
        allRecords[TopicPartition(topic, defaultPartition)] = recordsForSingleTopic

        return ConsumerRecords(allRecords)
    }
}
