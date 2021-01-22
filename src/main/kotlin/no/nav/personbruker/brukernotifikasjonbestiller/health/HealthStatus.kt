package no.nav.personbruker.brukernotifikasjonbestiller.health

data class HealthStatus(val serviceName: String,
                          val status: Status,
                          val statusMessage: String,
                          val includeInReadinesss: Boolean = true)

enum class Status {
    OK, ERROR
}

