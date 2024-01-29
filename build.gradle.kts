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
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    mavenLocal()
}

dependencies {
    implementation("no.nav.tms:brukernotifikasjon-schemas:2.6.0")
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(Kafka.clients)
    implementation(Avro.avroSerializer)
    implementation(Logstash.logbackEncoder)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.logback)
    implementation(Postgresql.postgresql)
    implementation(Ktor.Serialization.jackson)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.defaultHeaders)
    implementation(JacksonDatatype.datatypeJsr310)
    implementation(KotlinLogging.logging)
    implementation(SulkyUlid.sulkyUlid)
    implementation(TmsCommonLib.utils)
    implementation(TmsCommonLib.observability)
    implementation(Varsel.kotlinBuilder)


    testImplementation(Junit.api)
    testImplementation(Junit.params)
    testImplementation(Mockk.mockk)
    testImplementation(Jjwt.api)
    testImplementation(TestContainers.postgresql)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)

    testRuntimeOnly(Junit.engine)
}

application {
    mainClassName = "no.nav.personbruker.dittnav.brukernotifikasjonbestiller.ApplicationKt"
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }
}

apply(plugin = Shadow.pluginId)
