package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.personbruker.dittnav.common.util.config.IntEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar
import java.net.URL

data class Environment(
        val bootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMAREGISTRY_SERVERS"),
        val username: String = getEnvVar("SERVICEUSER_USERNAME"),
        val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
        val groupId: String = getEnvVar("GROUP_ID"),
        val applicationName: String = "dittnav-brukernotifikasjonbestiller",
        val eventHandlerURL: URL = URL(getEnvVar("EVENT_HANDLER_URL").trimEnd('/')),
        val clusterName: String = getEnvVar("NAIS_CLUSTER_NAME"),
        val namespace: String = getEnvVar("NAIS_NAMESPACE"),
        val sensuHost: String = getEnvVar("SENSU_HOST"),
        val sensuPort: Int = IntEnvVar.getEnvVarAsInt("SENSU_PORT"),
        val sensuBatchingEnabled: Boolean = getEnvVar("SENSU_BATCHING_ENABLED", "true").toBoolean(),
        val sensuBatchesPerSecond: Int = getEnvVar("SENSU_BATCHING_ENABLED", "3").toInt(),
        val aivenBrokers: String = getEnvVar("KAFKA_BROKERS"),
        val aivenTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        val aivenKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
        val aivenCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        val aivenSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        val aivenSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        val aivenSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
        val dbUser: String = getEnvVar("DB_USERNAME"),
        val dbPassword: String = getEnvVar("DB_PASSWORD"),
        val dbHost: String = getEnvVar("DB_HOST"),
        val dbPort: String = getEnvVar("DB_PORT"),
        val dbName: String = getEnvVar("DB_DATABASE"),
        val dbUrl: String ="jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
)

fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}

fun shouldPollBeskjed() = StringEnvVar.getOptionalEnvVar("POLL_BESKJED", "false").toBoolean()

fun shouldPollOppgave() = StringEnvVar.getOptionalEnvVar("POLL_OPPGAVE", "false").toBoolean()

fun shouldPollStatusoppdatering() = StringEnvVar.getOptionalEnvVar("POLL_STATUSOPPDATERING", "false").toBoolean()

fun shouldPollDone() = StringEnvVar.getOptionalEnvVar("POLL_DONE", "false").toBoolean()
