package no.nav.personbruker.brukernotifikasjonbestiller.health

interface HealthCheck {

    suspend fun status(): HealthStatus

}
