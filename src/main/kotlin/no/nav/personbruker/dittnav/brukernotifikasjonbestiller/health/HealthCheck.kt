package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

interface HealthCheck {

    suspend fun status(): HealthStatus

}
