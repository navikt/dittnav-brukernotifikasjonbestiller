package no.nav.personbruker.brukernotifikasjonbestiller.config

import no.nav.personbruker.brukernotifikasjonbestiller.health.HealthService

class ApplicationContext {

    val healthService = HealthService(this)

}
