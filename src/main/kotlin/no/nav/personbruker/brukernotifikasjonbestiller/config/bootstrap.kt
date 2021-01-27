package no.nav.personbruker.brukernotifikasjonbestiller.config

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.prometheus.client.hotspot.DefaultExports
import no.nav.personbruker.brukernotifikasjonbestiller.health.healthApi

@KtorExperimentalAPI
fun Application.mainModule(appContext: ApplicationContext = ApplicationContext()) {
    val environment = Environment()
    DefaultExports.initialize()
    install(DefaultHeaders)
    routing {
        healthApi(appContext.healthService)

        get("/usikret") {
            call.respondText(text = "Usikret API.", contentType = ContentType.Text.Plain)
        }

        configureShutdownHook(appContext)
    }
}

private fun Application.configureShutdownHook(appContext: ApplicationContext) {

}
