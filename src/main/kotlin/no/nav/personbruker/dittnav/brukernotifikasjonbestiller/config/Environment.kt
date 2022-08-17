package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.personbruker.dittnav.common.util.config.IntEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVarAsList

data class Environment(
        val groupId: String = getEnvVar("GROUP_ID"),
        val applicationName: String = "dittnav-brukernotifikasjonbestiller",
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
        val dbUrl: String = getDbUrl(dbHost, dbPort, dbName),
        val beskjedInternTopicName: String = getEnvVar("INTERN_BESKJED_TOPIC"),
        val oppgaveInternTopicName: String = getEnvVar("INTERN_OPPGAVE_TOPIC"),
        val innboksInternTopicName: String = getEnvVar("INTERN_INNBOKS_TOPIC"),
        val doneInternTopicName: String = getEnvVar("INTERN_DONE_TOPIC"),
        val beskjedInputTopicName: String = getEnvVar("OPEN_INPUT_BESKJED_TOPIC"),
        val oppgaveInputTopicName: String = getEnvVar("OPEN_INPUT_OPPGAVE_TOPIC"),
        val innboksInputTopicName: String = getEnvVar("OPEN_INPUT_INNBOKS_TOPIC"),
        val doneInputTopicName: String = getEnvVar("OPEN_INPUT_DONE_TOPIC"),
        val feilresponsTopicName: String = getEnvVar("FEILRESPONS_TOPIC"),
        val serviceUserMapping: List<String> = getEnvVarAsList("SERVICEUSER_MAPPING"),

        val produceToRapid: Boolean = getEnvVar("PRODUCE_TO_RAPID", "false").toBoolean(),
        val rapidTopic: String = "min-side.brukervarsel-v1",
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

fun shouldPollBeskjedInput() = StringEnvVar.getOptionalEnvVar("POLL_BESKJED_INPUT", "false").toBoolean()

fun shouldPollOppgaveInput() = StringEnvVar.getOptionalEnvVar("POLL_OPPGAVE_INPUT", "false").toBoolean()

fun shouldPollInnboksInput() = StringEnvVar.getOptionalEnvVar("POLL_INNBOKS_INPUT", "false").toBoolean()

fun shouldPollDoneInput() = StringEnvVar.getOptionalEnvVar("POLL_DONE_INPUT", "false").toBoolean()

fun getDbUrl(host: String, port: String, name: String): String {
        return if (host.endsWith(":$port")) {
                "jdbc:postgresql://${host}/$name"
        } else {
                "jdbc:postgresql://${host}:${port}/${name}"
        }
}
