package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.AvroBeskjedInputObjectMother
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.AvroNokkelInputObjectMother
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    private val defaultPartition = 1
    private val defaultOffset = 1

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<NokkelInput, BeskjedInput> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelInput, BeskjedInput>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<NokkelInput, BeskjedInput>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelInput, BeskjedInput>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = AvroBeskjedInputObjectMother.createBeskjedInput()
            val nokkel = AvroNokkelInputObjectMother.createNokkelInputWithEventId(i.toString())

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }

    fun <T> createInputConsumerRecords(nokkel: NokkelInput?, event: T?, topic: String): ConsumerRecords<NokkelInput, T> {
        val allRecords = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelInput, T>>>()
        val recordsForSingleTopic = mutableListOf<ConsumerRecord<NokkelInput, T>>()

        recordsForSingleTopic.add(ConsumerRecord(topic, defaultPartition, defaultOffset.toLong(), nokkel, event))
        allRecords[TopicPartition(topic, defaultPartition)] = recordsForSingleTopic

        return ConsumerRecords(allRecords)
    }
}
