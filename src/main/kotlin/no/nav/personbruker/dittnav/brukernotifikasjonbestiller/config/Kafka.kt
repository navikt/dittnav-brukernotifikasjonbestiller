package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.netty.util.NetUtil
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.kafka.serializer.SwallowSerializationErrorsAvroDeserializer
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.InetSocketAddress
import java.util.*

object Kafka {

    private val log: Logger = LoggerFactory.getLogger(Kafka::class.java)

    private const val transactionIdName = "dittnav-brukernotifikasjonbestiller-transaction"

    const val beskjedInputTopicName = "aapen-brukernotifikasjon-nyBeskjed-v1"
    const val beskjedHovedTopicName = "min-side.privat-brukernotifikasjon-beskjed-v4"

    const val oppgaveInputTopicName = "aapen-brukernotifikasjon-nyOppgave-v1"
    const val oppgaveHovedTopicName = "min-side.privat-brukernotifikasjon-oppgave-v3"

    const val statusoppdateringInputTopicName = "aapen-brukernotifikasjon-nyStatusoppdatering-v1"
    const val statusoppdateringHovedTopicName = "min-side.privat-brukernotifikasjon-statusoppdatering-v3"

    const val doneInputTopicName = "aapen-brukernotifikasjon-done-v1"
    const val doneHovedTopicName = "min-side.privat-brukernotifikasjon-done-v3"

    const val feilresponsTopicName = "min-side.aapen-brukernotifikasjon-feilrespons-v1"

    fun consumerProps(env: Environment, eventtypeToConsume: Eventtype, enableSecurity: Boolean = isCurrentlyRunningOnNais()): Properties {
        val groupIdAndEventType = buildGroupIdIncludingEventType(env, eventtypeToConsume)
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServers)
            put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupIdAndEventType)
            put(ConsumerConfig.CLIENT_ID_CONFIG, groupIdAndEventType + NetUtil.getHostname(InetSocketAddress(0)))
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, SwallowSerializationErrorsAvroDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SwallowSerializationErrorsAvroDeserializer::class.java)
            put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
            if (enableSecurity) {
                putAll(credentialPropsOnPrem(env))
            }
        }
    }

    fun producerProps(env: Environment, type: Eventtype): Properties {
        return Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.aivenBrokers)
            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.aivenSchemaRegistry)
            put(ProducerConfig.CLIENT_ID_CONFIG, env.groupId + type + NetUtil.getHostname(InetSocketAddress(0)))
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, buildTransactionIdName(type))
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
            if (env.securityConfig.enabled) {
                putAll(credentialPropsAiven(env.securityConfig.variables!!))
            }
        }
    }

    fun producerFeilresponsProps(env: Environment, eventtype: Eventtype): Properties {
        return Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.aivenBrokers)
            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.aivenSchemaRegistry)
            put(ProducerConfig.CLIENT_ID_CONFIG, env.groupId + Eventtype.FEILRESPONS + eventtype + NetUtil.getHostname(InetSocketAddress(0)))
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, buildTransactionIdNameFeilrespons(eventtype))
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
            if (env.securityConfig.enabled) {
                putAll(credentialPropsAiven(env.securityConfig.variables!!))
            }
        }
    }

    private fun credentialPropsOnPrem(env: Environment): Properties {
        return Properties().apply {
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_JAAS_CONFIG,
                    """org.apache.kafka.common.security.plain.PlainLoginModule required username="${env.username}" password="${env.password}";""")
            System.getenv("NAV_TRUSTSTORE_PATH")?.let {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
                log.info("Configured ${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG} location")
            }
        }
    }
    private fun credentialPropsAiven(securityVars: SecurityVars): Properties {
        return Properties().apply {
            put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, "${securityVars.aivenSchemaRegistryUser}:${securityVars.aivenSchemaRegistryPassword}")
            put(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, securityVars.aivenTruststorePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, securityVars.aivenCredstorePassword)
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, securityVars.aivenKeystorePath)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, securityVars.aivenCredstorePassword)
            put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, securityVars.aivenCredstorePassword)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        }
    }

    private fun buildGroupIdIncludingEventType(env: Environment, eventtypeToConsume: Eventtype) =
            env.groupId + eventtypeToConsume.eventtype

    private fun buildTransactionIdName(eventtype: Eventtype) =
            "$transactionIdName-${eventtype.eventtype}"

    private fun buildTransactionIdNameFeilrespons(eventtype: Eventtype) =
            "$transactionIdName-${Eventtype.FEILRESPONS}-${eventtype.eventtype}"
}
