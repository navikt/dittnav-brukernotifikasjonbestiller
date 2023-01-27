package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.objectmother

import no.nav.brukernotifikasjon.schemas.input.BeskjedInput
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.beskjed.BeskjedTestData
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.nokkel.NokkelTestData
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition

object ConsumerRecordsObjectMother {

    fun giveMeANumberOfBeskjedRecords(numberOfRecords: Int, topicName: String): ConsumerRecords<NokkelInput, BeskjedInput> {
        val records = mutableMapOf<TopicPartition, List<ConsumerRecord<NokkelInput, BeskjedInput>>>()
        val recordsForSingleTopic = createBeskjedRecords(topicName, numberOfRecords)
        records[TopicPartition(topicName, numberOfRecords)] = recordsForSingleTopic
        return ConsumerRecords(records)
    }

    private fun createBeskjedRecords(topicName: String, totalNumber: Int): List<ConsumerRecord<NokkelInput, BeskjedInput>> {
        val allRecords = mutableListOf<ConsumerRecord<NokkelInput, BeskjedInput>>()
        for (i in 0 until totalNumber) {
            val schemaRecord = BeskjedTestData.beskjedInput()
            val nokkel = NokkelTestData.createNokkelInputWithEventId(i.toString())

            allRecords.add(ConsumerRecord(topicName, i, i.toLong(), nokkel, schemaRecord))
        }
        return allRecords
    }
}
