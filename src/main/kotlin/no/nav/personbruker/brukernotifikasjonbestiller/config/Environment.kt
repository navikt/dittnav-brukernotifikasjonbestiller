package no.nav.personbruker.brukernotifikasjonbestiller.config

import no.nav.personbruker.dittnav.common.util.config.StringEnvVar
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar

data class Environment(
        val bootstrapServers: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS"),
        val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMAREGISTRY_SERVERS"),
        val username: String = getEnvVar("SERVICEUSER_USERNAME"),
        val password: String = getEnvVar("SERVICEUSER_PASSWORD"),
        val groupId: String = getEnvVar("GROUP_ID"),
        val applicationName: String = "dittnav-brukernotifikasjonbestiller"
)

fun isCurrentlyRunningOnNais(): Boolean {
    return System.getenv("NAIS_APP_NAME") != null
}

fun shouldPollBeskjed() = StringEnvVar.getOptionalEnvVar("POLL_BESKJED", "false").toBoolean()

fun shouldPollOppgave() = StringEnvVar.getOptionalEnvVar("POLL_OPPGAVE", "false").toBoolean()

fun shouldPollStatusoppdatering() = StringEnvVar.getOptionalEnvVar("POLL_STATUSOPPDATERING", "false").toBoolean()

fun shouldPollDone() = StringEnvVar.getOptionalEnvVar("POLL_DONE", "false").toBoolean()
