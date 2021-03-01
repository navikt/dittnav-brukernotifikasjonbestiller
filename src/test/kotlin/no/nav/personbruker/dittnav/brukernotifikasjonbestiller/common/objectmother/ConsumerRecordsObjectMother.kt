package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    private val defaultTopicName = "topic-name-test"
    private val defaultPartition = 1
    private val defaultOffset = 1

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<Nokkel, Beskjed> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, Beskjed>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<Nokkel, Beskjed>> {
        val allRecords = mutableListOf<ConsumerRecord<Nokkel, Beskjed>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedObjectMother.createBeskjed(i)
            val nokkel = AvroNokkelObjectMother.createNokkelWithEventId(i.toString())

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    fun <T> createConsumerRecords(nokkel: Nokkel?, event: T?): ConsumerRecords<Nokkel, T> {
        val allRecords = mutableMapOf<TopicPartition, List<ConsumerRecord<Nokkel, T>>>()
        val recordsForSingleTopic = mutableListOf<ConsumerRecord<Nokkel, T>>()

        recordsForSingleTopic.add(ConsumerRecord(defaultTopicName, defaultPartition, defaultOffset.toLong(), nokkel, event))
        allRecords[TopicPartition(defaultTopicName, defaultPartition)] = recordsForSingleTopic

        return ConsumerRecords(allRecords)
    }
}
