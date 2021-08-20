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
        val influxdbHost: String = getEnvVar("INFLUXDB_HOST"),
        val influxdbPort: Int = IntEnvVar.getEnvVarAsInt("INFLUXDB_PORT"),
        val influxdbName: String = getEnvVar("INFLUXDB_DATABASE_NAME"),
        val influxdbUser: String = getEnvVar("INFLUXDB_USER"),
        val influxdbPassword: String = getEnvVar("INFLUXDB_PASSWORD"),
        val influxdbRetentionPolicy: String = getEnvVar("INFLUXDB_RETENTION_POLICY"),
        val aivenBrokers: String = getEnvVar("KAFKA_BROKERS"),
        val aivenSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        val securityConfig: SecurityConfig = SecurityConfig(isCurrentlyRunningOnNais()),
        val dbUser: String = getEnvVar("DB_USERNAME"),
        val dbPassword: String = getEnvVar("DB_PASSWORD"),
        val dbHost: String = getEnvVar("DB_HOST"),
        val dbPort: String = getEnvVar("DB_PORT"),
        val dbName: String = getEnvVar("DB_DATABASE"),
        val dbUrl: String = getDbUrl(dbHost, dbPort, dbName)
)

data class SecurityConfig(
        val enabled: Boolean,

        val variables: SecurityVars? = if (enabled) {
                SecurityVars()
        } else {
                null
        }
)

data class SecurityVars(
        val aivenTruststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        val aivenKeystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
        val aivenCredstorePassword: String = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        val aivenSchemaRegistryUser: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        val aivenSchemaRegistryPassword: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD")
)


fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}

fun shouldPollBeskjed() = StringEnvVar.getOptionalEnvVar("POLL_BESKJED", "false").toBoolean()

fun shouldPollOppgave() = StringEnvVar.getOptionalEnvVar("POLL_OPPGAVE", "false").toBoolean()

fun shouldPollInnboks() = StringEnvVar.getOptionalEnvVar("POLL_INNBOKS", "false").toBoolean()

fun shouldPollStatusoppdatering() = StringEnvVar.getOptionalEnvVar("POLL_STATUSOPPDATERING", "false").toBoolean()

fun shouldPollDone() = StringEnvVar.getOptionalEnvVar("POLL_DONE", "false").toBoolean()

fun getDbUrl(host: String, port: String, name: String): String {
        return if (host.endsWith(":$port")) {
                "jdbc:postgresql://${host}/$name"
        } else {
                "jdbc:postgresql://${host}:${port}/${name}"
        }
}
