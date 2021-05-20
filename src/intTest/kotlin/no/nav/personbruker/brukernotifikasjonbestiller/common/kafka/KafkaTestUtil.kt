package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka

import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Environment
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
                influxdbHost = "INFLUXDB_HOST_IkkeIBrukHer",
                influxdbPort = 0,
                influxdbName = "INFLUXDB_DATABASE_NAME_IkkeIBrukHer",
                influxdbUser = "INFLUXDB_USER_IkkeIBrukHer",
                influxdbPassword = "INFLUXDB_PASSWORD_IkkeIBrukHer",
                influxdbRetentionPolicy = "INFLUXDB_RETENTION_POLICY_IkkeIBrukHer",
                aivenBrokers = embeddedEnv.brokersURL.substringAfterLast("/"),
                aivenTruststorePath = "KAFKA_TRUSTSTORE_PATH",
                aivenKeystorePath = "KAFKA_KEYSTORE_PATH",
                aivenCredstorePassword = "KAFKA_CREDSTORE_PASSWORD",
                aivenSchemaRegistry = embeddedEnv.schemaRegistry!!.url,
                aivenSchemaRegistryUser = username,
                aivenSchemaRegistryPassword = password
        )
    }

    suspend fun produceEvents(env: Environment, topicName: String, events: Map<Nokkel, GenericRecord>): Boolean {
        return KafkaProducerUtil.kafkaAvroProduce(
                env.bootstrapServers,
                env.schemaRegistryUrl,
                topicName,
                env.username,
                env.password,
                events)
    }

}
