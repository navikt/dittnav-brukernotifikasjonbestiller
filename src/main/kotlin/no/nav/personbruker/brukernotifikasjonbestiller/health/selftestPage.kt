package no.nav.personbruker.brukernotifikasjonbestiller.health

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.html.*
import no.nav.personbruker.brukernotifikasjonbestiller.config.Environment
import no.nav.personbruker.brukernotifikasjonbestiller.config.HttpClientBuilder

suspend fun ApplicationCall.pingDependencies(environment: Environment) = coroutineScope {
    val client = HttpClientBuilder.build()

    var services = emptyMap<String, SelftestStatus>()

    client.close()

    val serviceStatus = if (services.values.any { it.status == Status.ERROR }) Status.ERROR else Status.OK

    respondHtml(status =
    if(Status.ERROR == serviceStatus) {
        HttpStatusCode.ServiceUnavailable
    } else {
        HttpStatusCode.OK
    })
    {
        head {
            title { +"Selftest dittnav-brukernotifikasjonbestiller" }
        }
        body {
            h1 {
                style = if (serviceStatus == Status.OK) "background: green" else "background: red;font-weight:bold"
                +"Service status: $serviceStatus"
            }
            table {
                thead {
                    tr { th { +"SELFTEST dittnav-brukernotifikasjonbestiller" } }
                }
                tbody {
                    services.map {
                        tr {
                            td { +it.key }
                            td { +it.value.pingedURL.toString() }
                            td {
                                style = if (it.value.status == Status.OK) "background: green" else "background: red;font-weight:bold"
                                +it.value.status.toString()
                            }
                            td { +it.value.statusMessage }
                        }
                    }
                }
            }
        }
    }
}
