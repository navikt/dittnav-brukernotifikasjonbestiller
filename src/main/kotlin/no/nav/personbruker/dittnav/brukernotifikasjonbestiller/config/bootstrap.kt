package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.routing.*
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
            appContext.internBeskjedKafkaProducerLegacy.flushAndClose()
            appContext.internOppgaveKafkaProducerLegacy.flushAndClose()
            appContext.internInnboksKafkaProducerLegacy.flushAndClose()
            appContext.internDoneKafkaProducerLegacy.flushAndClose()
            appContext.internStatusoppdateringKafkaProducerLegacy.flushAndClose()
            appContext.periodicConsumerPollingCheck.stop()
        }
        appContext.database.dataSource.close()
    }
}
