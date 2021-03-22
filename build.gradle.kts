import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    kotlin("jvm").version(Kotlin.version)
    kotlin("plugin.allopen").version(Kotlin.version)

    id(Shadow.pluginId) version (Shadow.version)
    // Apply the application plugin to add support for building a CLI application.
    application
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    implementation(Brukernotifikasjon.schemas)
    implementation(Brukernotifikasjon.schemas_internal)
    implementation(DittNAV.Common.influx)
    implementation(DittNAV.Common.utils)
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(Kafka.Apache.clients)
    implementation(Kafka.Confluent.avroSerializer)
    implementation(Ktor.htmlBuilder)
    implementation(Ktor.serverNetty)
    implementation(Logback.classic)
    implementation(Logstash.logbackEncoder)
    implementation(NAV.tokenValidatorKtor)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.logback)
    implementation(Postgresql.postgresql)
    implementation(ULID.sulkyUlid)
    implementation(Ktor.clientApache)
    implementation(Ktor.clientJson)
    implementation(Ktor.clientJackson)
    implementation(Jackson.dataTypeJsr310)

    testImplementation(H2Database.h2)
    testImplementation(Junit.api)
    testImplementation(Ktor.clientMock)
    testImplementation(Ktor.clientMockJvm)
    testImplementation(Kluent.kluent)
    testImplementation(Mockk.mockk)
    testImplementation(Jjwt.api)

    testRuntimeOnly(Junit.engine)
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }

    register("runServer", JavaExec::class) {
        println("Setting default environment variables for running with DittNAV docker-compose")

        environment("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
        environment("KAFKA_SCHEMAREGISTRY_SERVERS", "http://localhost:8081")
        environment("SERVICEUSER_USERNAME", "username")
        environment("SERVICEUSER_PASSWORD", "password")
        environment("GROUP_ID", "dittnav_brukernotifikasjonbestiller")

        environment("CORS_ALLOWED_ORIGINS", "localhost:9002")

        environment("LOGINSERVICE_IDPORTEN_DISCOVERY_URL", "http://localhost:9000/.well-known/openid-configuration")
        environment("LOGINSERVICE_IDPORTEN_AUDIENCE", "stubOidcClient")
        environment("OIDC_CLAIM_CONTAINING_THE_IDENTITY", "pid")

        environment("NAIS_CLUSTER_NAME", "dev-sbs")
        environment("NAIS_NAMESPACE", "personbruker")
        environment("SENSU_HOST", "stub")
        environment("SENSU_PORT", "")

        environment("DB_HOST", "localhost")
        environment("DB_PORT", "5434")
        environment("DB_DATABASE", "brukernotifikasjonbestiller-cache")
        environment("DB_USERNAME", "dittnav-brukernotifikasjonbestiller-user")
        environment("DB_PASSWORD", "testpassword")


        main = application.mainClassName
        classpath = sourceSets["main"].runtimeClasspath
    }
}

apply(plugin = Shadow.pluginId)
