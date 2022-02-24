package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.brukernotifikasjon.schemas.input.NokkelInput
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Environment
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.SecurityConfig
import org.apache.avro.generic.GenericRecord
import java.net.URL
import java.util.*

object KafkaTestUtil {

    val username = "srvkafkaclient"
    val password = "kafkaclient"

    fun createDefaultKafkaEmbeddedInstance(topics: List<String>): KafkaEnvironment {
        return KafkaEnvironment(
                topicNames = topics,
                withSecurity = false,
                withSchemaRegistry = true,
                users = listOf(JAASCredential(username, password)),
                brokerConfigOverrides = Properties().apply {
                    put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                    put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                }
        )
    }

    fun createEnvironmentForEmbeddedKafka(embeddedEnv: KafkaEnvironment): Environment {
        return Environment(
                bootstrapServers = embeddedEnv.brokersURL.substringAfterLast("/"),
                schemaRegistryUrl = embeddedEnv.schemaRegistry!!.url,
                username = username,
                password = password,
                groupId = "groupId-for-tests",
                applicationName = "dittnav-brukernotifikasjonbestiller",
                dbHost = "dbHostIkkeIBrukHer",
                dbPort = "dbPortIkkeIBrukHer",
                dbName = "dbNameIkkeIBrukHer",
                dbUrl = "dbUrlIkkeIBrukHer",
                dbUser = "dbUserIkkeIBrukHer",
                dbPassword = "dbPWIkkeIBrukHer",
                clusterName = "clusterNameIkkeIBrukHer",
                namespace = "namespaceIkkeIBrukHer",
                eventHandlerURL = URL("http://event-handler"),
                influxdbHost = "influxdbHostIkkeIBrukHer",
                influxdbPort = 0,
                influxdbName = "influxdbDatabaseNameIkkeIBrukHer",
                influxdbUser = "influxdbUserIkkeIBrukHer",
                influxdbPassword = "influxdbPasswordIkkeIBrukHer",
                influxdbRetentionPolicy = "influxdbRetentionPolicyIkkeIBrukHer",
                aivenBrokers = embeddedEnv.brokersURL.substringAfterLast("/"),
                aivenSchemaRegistry = embeddedEnv.schemaRegistry!!.url,
                securityConfig = SecurityConfig(enabled = false),
                serviceUserMapping = listOf(""),
                beskjedInternTopicName = KafkaTestTopics.beskjedInternTopicName,
                oppgaveInternTopicName = KafkaTestTopics.oppgaveInternTopicName,
                innboksInternTopicName = KafkaTestTopics.innboksInternTopicName,
                statusoppdateringInternTopicName = KafkaTestTopics.statusoppdateringInternTopicName,
                doneInternTopicName = KafkaTestTopics.doneInternTopicName,
                beskjedInputTopicName = KafkaTestTopics.beskjedInputTopicName,
                oppgaveInputTopicName = KafkaTestTopics.oppgaveInputTopicName,
                innboksInputTopicName = KafkaTestTopics.innboksInputTopicName,
                statusoppdateringInputTopicName = KafkaTestTopics.statusoppdateringInputTopicName,
                doneInputTopicName = KafkaTestTopics.doneInputTopicName,
                feilresponsTopicName = KafkaTestTopics.feilresponsTopicName,
                beskjedLegacyTopicName = KafkaTestTopics.beskjedLegacyTopicName,
                oppgaveLegacyTopicName = KafkaTestTopics.oppgaveLegacyTopicName,
                innboksLegacyTopicName = KafkaTestTopics.innboksLegacyTopicName,
                statusoppdateringLegacyTopicName = KafkaTestTopics.statusoppdateringLegacyTopicName,
                doneLegacyTopicName = KafkaTestTopics.doneLegacyTopicName
        )
    }

    suspend fun produceEventsLegacy(env: Environment, topicName: String, events: Map<Nokkel, GenericRecord>): Boolean {
        return KafkaProducerUtil.kafkaAvroProduceLegacy(
                env.bootstrapServers,
                env.schemaRegistryUrl,
                topicName,
                events)
    }

    suspend fun produceEventsInput(env: Environment, topicName: String, events: Map<NokkelInput, GenericRecord>): Boolean {
        return KafkaProducerUtil.kafkaAvroProduceInput(
                env.bootstrapServers,
                env.schemaRegistryUrl,
                topicName,
                events)
    }
}
