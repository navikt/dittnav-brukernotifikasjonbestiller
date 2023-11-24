package no.nav.personbruker.dittnav.brukernotifikasjonbestiller

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.Environment
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.Kafka
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionForwarder
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.varsel.VarselActionProducer
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.AppHealthChecker
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun main() {


    val environment = Environment()

    val kafkaProducer = initializeKafkaProducer(environment)

    val varselRapidProducer = VarselActionProducer(
        kafkaProducer = kafkaProducer,
        topicName = environment.rapidTopic
    )

    val inputConsumer = InputTopicConsumer(
        topics = environment.inputTopics,
        kafkaConsumer = KafkaConsumer(Kafka.consumerProps(environment)),
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

private fun initializeKafkaProducer(environment: Environment) = KafkaProducer<String, String>(
    Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.aivenBrokers)
        put(
            ProducerConfig.CLIENT_ID_CONFIG,
            "dittnav-brukernotifikasjonbestiller"
        )
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        putAll(Kafka.credentialPropsAiven(environment.securityConfig.variables!!))
    }
)
