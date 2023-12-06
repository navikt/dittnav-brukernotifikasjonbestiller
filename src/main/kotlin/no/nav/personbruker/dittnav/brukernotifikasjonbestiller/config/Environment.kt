package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.tms.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val groupId: String = "dittnav-brukernotifikasjonbestiller-dev-01",
    val aivenBrokers: String = getEnvVar("KAFKA_BROKERS"),
    val aivenSchemaRegistry: String = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
    val securityConfig: SecurityConfig = SecurityConfig(),
    val beskjedInputTopicName: String = "min-side.aapen-brukernotifikasjon-beskjed-v1",
    val oppgaveInputTopicName: String = "min-side.aapen-brukernotifikasjon-oppgave-v1",
    val innboksInputTopicName: String = "min-side.aapen-brukernotifikasjon-innboks-v1",
    val doneInputTopicName: String = "min-side.aapen-brukernotifikasjon-done-v1",

    val varselTopic: String = "min-side.aapen-brukervarsel-v1",
) {
    val inputTopics get() = listOf(
        beskjedInputTopicName,
        oppgaveInputTopicName,
        innboksInputTopicName,
        doneInputTopicName
    )
}

data class SecurityConfig(
    val enabled: Boolean = isCurrentlyRunningOnNais(),

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
