package no.nav.personbruker.brukernotifikasjonbestiller.health

import no.nav.personbruker.brukernotifikasjonbestiller.config.ApplicationContext

class HealthService(private val applicationContext: ApplicationContext) {

    suspend fun getHealthChecks(): List<HealthStatus> {
        return emptyList()
    }

}
