package no.nav.personbruker.brukernotifikasjonbestiller.config

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.netty.util.NetUtil
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

    const val beskjedInputTopicName = "aapen-brukernotifikasjon-nyBeskjed-v1"
    const val beskjedMainTopicName = "privat-brukernotifikasjon-nyBeskjed-v1"

    const val oppgaveInputTopicName = "aapen-brukernotifikasjon-nyOppgave-v1"
    const val oppgaveMainTopicName = "privat-brukernotifikasjon-nyOppgave-v1"

    const val statusoppdateringInputTopicName = "aapen-brukernotifikasjon-nyStatusoppdatering-v1"
    const val statusoppdateringMainTopicName = "privat-brukernotifikasjon-nyStatusoppdatering-v1"

    const val doneInputTopicName = "aapen-brukernotifikasjon-done-v1"
    const val doneMainTopicName = "privat-brukernotifikasjon-done-v1"

    fun consumerProps(env: Environment, eventtypeToConsume: Eventtype, enableSecurity: Boolean = isCurrentlyRunningOnNais()): Properties {
        val groupIdAndEventType = buildGroupIdIncludingEventType(env, eventtypeToConsume)
        return Properties().apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, groupIdAndEventType)
            put(ConsumerConfig.CLIENT_ID_CONFIG, groupIdAndEventType + NetUtil.getHostname(InetSocketAddress(0)))
            commonProps(env, enableSecurity)
        }
    }

    fun producerProps(env: Environment, enableSecurity: Boolean = isCurrentlyRunningOnNais()): Properties {
        return Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServers)
            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
            put(ProducerConfig.CLIENT_ID_CONFIG, env.groupId + NetUtil.getHostname(InetSocketAddress(0)))
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000)
            if (enableSecurity) {
                putAll(credentialProps(env))
            }
        }
    }

    private fun Properties.commonProps(env: Environment, enableSecurity: Boolean) {
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServers)
        put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, env.schemaRegistryUrl)
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
        put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)
        if (enableSecurity) {
            putAll(credentialProps(env))
        }
    }

    private fun credentialProps(env: Environment): Properties {
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

    private fun buildGroupIdIncludingEventType(env: Environment, eventTypeToConsume: Eventtype) = env.groupId + eventTypeToConsume.eventtype

}
