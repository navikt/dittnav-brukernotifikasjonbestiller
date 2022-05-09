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
    kotlinOptions.jvmTarget = "17"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
    mavenLocal()
}

dependencies {
    implementation("com.github.navikt:brukernotifikasjon-schemas:1.2022.04.26-11.25-7155b5142c85")
    implementation("com.github.navikt:brukernotifikasjon-schemas-internal:1.2022.01.20-12.20-9c37cb170dc9")
    implementation(DittNAV.Common.influxdb)
    implementation(DittNAV.Common.utils)
    implementation(DittNAV.Common.logging)
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

    testImplementation(Junit.api)
    testImplementation(Ktor.clientMock)
    testImplementation(Ktor.clientMockJvm)
    testImplementation(Mockk.mockk)
    testImplementation(Jjwt.api)
    testImplementation(Kotlinx.atomicfu)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)

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

        DockerComposeDefaults.environomentVariables.forEach { (name, value) ->
            println("Setting the environment variable $name")
            environment(name, value)
        }

        environment("GROUP_ID", "dittnav_brukernotifikasjonbestiller")
        environment("DB_HOST", "localhost")
        environment("DB_PORT", "5434")
        environment("DB_DATABASE", "brukernotifikasjonbestiller")
        environment("DB_USERNAME", "dittnav-brukernotifikasjonbestiller-user")

        main = application.mainClassName
        classpath = sourceSets["main"].runtimeClasspath
    }
}

apply(plugin = Shadow.pluginId)
