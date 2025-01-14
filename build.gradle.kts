import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm").version(Kotlin.version)

    id(TmsJarBundling.plugin)

    application
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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
    implementation(Kafka.clients)
    implementation(Avro.avroSerializer)
    implementation(Logstash.logbackEncoder)
    implementation(Prometheus.metricsCore)
    implementation(Prometheus.exporterCommon)
    implementation(Postgresql.postgresql)
    implementation(Ktor.Server.netty)
    implementation(Ktor.Server.defaultHeaders)
    implementation(KotlinLogging.logging)
    implementation(TmsCommonLib.utils)
    implementation(TmsCommonLib.observability)
    implementation(Varsel.kotlinBuilder)

    testImplementation(JunitPlatform.launcher)
    testImplementation(JacksonDatatype.datatypeJsr310)
    testImplementation(JunitJupiter.api)
    testImplementation(JunitJupiter.params)
    testImplementation(Mockk.mockk)
    testImplementation(Kotest.runnerJunit5)
    testImplementation(Kotest.assertionsCore)
}

application {
    mainClass.set("no.nav.personbruker.dittnav.brukernotifikasjonbestiller.ApplicationKt")
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
