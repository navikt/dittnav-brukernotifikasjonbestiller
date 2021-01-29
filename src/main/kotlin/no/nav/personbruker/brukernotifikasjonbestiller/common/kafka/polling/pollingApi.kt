package no.nav.personbruker.brukernotifikasjonbestiller.common.kafka.polling

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.personbruker.brukernotifikasjonbestiller.config.ApplicationContext
import no.nav.personbruker.brukernotifikasjonbestiller.config.KafkaConsumerSetup

fun Routing.pollingApi(appContext: ApplicationContext) {

    get("/internal/polling/start") {
        val responseText = "Polling etter eventer har blitt startet."
        KafkaConsumerSetup.restartPolling(appContext)
        call.respondText(text = responseText, contentType = ContentType.Text.Plain)
    }

    get("/internal/polling/stop") {
        val responseText = "All polling etter eventer har blitt stoppet."
        KafkaConsumerSetup.stopAllKafkaConsumers(appContext)
        call.respondText(text = responseText, contentType = ContentType.Text.Plain)
    }

}
