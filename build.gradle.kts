import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
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
    implementation(BrukernotifikasjonSchemas.internal)
    implementation(BrukernotifikasjonSchemas.input)
    implementation(DittNAVCommonLib.influxdb)
    implementation(DittNAVCommonLib.utils)
    implementation(Flyway.core)
    implementation(Hikari.cp)
    implementation(Kafka.clients)
    implementation(Avro.avroSerializer)
    implementation(Logback.classic)
    implementation(Logstash.logbackEncoder)
    implementation(NAV.tokenValidatorKtor)
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.logback)
    implementation(Postgresql.postgresql)
    implementation(SulkyUlid.sulkyUlid)
    implementation(Ktor2.Server.htmlDsl)
    implementation(Ktor2.Server.netty)
    implementation(Ktor2.Server.defaultHeaders)
    implementation(Ktor2.Serialization.jackson)
    implementation(JacksonDatatype.datatypeJsr310)

    testImplementation(Junit.api)
    testImplementation(Junit.params)
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
}

apply(plugin = Shadow.pluginId)
