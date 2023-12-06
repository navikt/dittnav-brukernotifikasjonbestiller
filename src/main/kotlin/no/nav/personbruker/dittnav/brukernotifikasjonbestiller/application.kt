package no.nav.personbruker.dittnav.brukernotifikasjonbestiller

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Environment
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionForwarder
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionProducer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.AppHealthChecker
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.Kafka.createProducer

fun main() {


    val environment = Environment()

    val kafkaProducer = createProducer(environment)

    val varselRapidProducer = VarselActionProducer(
        kafkaProducer = kafkaProducer,
        topicName = environment.varselTopic
    )

    val inputConsumer = InputTopicConsumer(
        topics = environment.inputTopics,
        kafkaConsumer = Kafka.createConsumer(environment),
        varselActionForwarder = VarselActionForwarder(varselRapidProducer)
    )

    val appHealthChecker = AppHealthChecker(inputConsumer)

    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment {
            module {
                brukernotifikasjonBestiller(
                    inputConsumer,
                    varselRapidProducer,
                    appHealthChecker
                )
            }
            connector {
                port = 8080
            }
        }
    ).start(wait = true)
}
