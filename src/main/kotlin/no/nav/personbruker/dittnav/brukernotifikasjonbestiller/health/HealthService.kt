package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.ApplicationContext

class HealthService(private val applicationContext: ApplicationContext) {

    suspend fun getHealthChecks(): List<HealthStatus> {
        return emptyList()
    }

}
