package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

fun Routing.healthApi(
    collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    appExpiryChecker: AppExpiryChecker = AppExpiryChecker()
) {

    val pingJsonResponse = """{"ping": "pong"}"""

    get("/internal/ping") {
        call.respondText(pingJsonResponse, ContentType.Application.Json)
    }

    get("/internal/isAlive") {
        if (appExpiryChecker.isExpired()) {
            call.respondText(
                text = "EXPIRED",
                status = HttpStatusCode.ServiceUnavailable,
                contentType = ContentType.Text.Plain
            )
        } else {
            call.respondText(text = "ALIVE", contentType = ContentType.Text.Plain)
        }
    }

    get("/internal/isReady") {
        call.respondText(text = "READY", contentType = ContentType.Text.Plain)
    }

    get("/metrics") {
        val names = call.request.queryParameters.getAll("name")?.toSet() ?: emptySet()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004), HttpStatusCode.OK) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
        }
    }
}
