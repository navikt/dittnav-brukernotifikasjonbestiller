package no.nav.personbruker.dittnav.brukernotifikasjonbestiller

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.AppHealthChecker
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.healthApi
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionProducer

fun Application.brukernotifikasjonBestiller(
    inputConsumer: InputTopicConsumer,
    varselActionProducer: VarselActionProducer,
    appHealthChecker: AppHealthChecker
) {
    DefaultExports.initialize()

    install(DefaultHeaders)

    routing {
        healthApi(appHealthChecker)
    }

    configureStartupHook(inputConsumer)
    configureShutdownHook(inputConsumer, varselActionProducer)
}

private fun Application.configureStartupHook(inputConsumer: InputTopicConsumer) {
    environment.monitor.subscribe(ApplicationStarted) {
        inputConsumer.startPolling()
    }
}

private fun Application.configureShutdownHook(inputConsumer: InputTopicConsumer, varselActionProducer: VarselActionProducer) {
    environment.monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            inputConsumer.stopPolling()
            varselActionProducer .flushAndClose()
        }
    }
}
