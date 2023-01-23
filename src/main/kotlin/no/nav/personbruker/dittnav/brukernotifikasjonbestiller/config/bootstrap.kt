package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health.healthApi

fun Application.mainModule(appContext: ApplicationContext = ApplicationContext()) {
    DefaultExports.initialize()

    install(DefaultHeaders)

    routing {
        healthApi(appContext.healthService)
    }
    configureStartupHook(appContext)
    configureShutdownHook(appContext)
}

private fun Application.configureStartupHook(appContext: ApplicationContext) {
    environment.monitor.subscribe(ApplicationStarted) {
        Flyway.runFlywayMigrations(appContext.environment)
        KafkaConsumerSetup.startAllKafkaPollers(appContext)
        appContext.periodicConsumerPollingCheck.start()
    }
}

private fun Application.configureShutdownHook(appContext: ApplicationContext) {
    environment.monitor.subscribe(ApplicationStopPreparing) {
        runBlocking {
            KafkaConsumerSetup.stopAllKafkaConsumers(appContext)
            appContext.internBeskjedKafkaProducer.flushAndClose()
            appContext.internOppgaveKafkaProducer.flushAndClose()
            appContext.internInnboksKafkaProducer.flushAndClose()
            appContext.internDoneKafkaProducer.flushAndClose()
            appContext.oppgaveRapidProducer.flushAndClose()
            appContext.innboksRapidProducer.flushAndClose()
            appContext.doneRapidProducer.flushAndClose()
            appContext.varselRapidProducer.flushAndClose()
            appContext.periodicConsumerPollingCheck.stop()
        }
        appContext.database.dataSource.close()
    }
}
