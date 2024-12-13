package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.prometheus.metrics.expositionformats.ExpositionFormats
import io.prometheus.metrics.model.registry.MetricNameFilter
import io.prometheus.metrics.model.registry.PrometheusRegistry

fun Routing.healthApi(
    appHealthChecker: AppHealthChecker
) {

    get("/internal/isAlive") {
        if (appHealthChecker.isUnhealthy()) {
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

    val writer = ExpositionFormats.init().openMetricsTextFormatWriter

    get("/metrics") {
        PrometheusRegistry.defaultRegistry.scrape()

        val requestedNames = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()

        val filter = if (requestedNames.isNotEmpty()) {
            MetricNameFilter.builder().nameMustBeEqualTo(requestedNames).build()
        } else {
            null
        }

        call.respondOutputStream(ContentType.parse(writer.contentType)) {
            writer.write(this, PrometheusRegistry.defaultRegistry.scrape(filter))
        }
    }
}
