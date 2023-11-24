package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config.AppHealthChecker
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class AppHealthCheckerTest {

    @Test
    fun `Should consider app as unhealthy if within expiry window and app lifetime is exceeded`() {
        val windowStart = LocalTime.now(ZoneOffset.UTC).hour
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 1200L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val consumer: InputTopicConsumer = mockk()

        every { consumer.isStopped() } returns false

        val expiryChecker = AppHealthChecker(
            consumer,
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isUnhealthy() shouldBe true
    }

    @Test
    fun `Should not consider app as unhealthy if within expiry window and app lifetime is not exceeded`() {
        val windowStart = LocalTime.now(ZoneOffset.UTC).hour
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 300L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val consumer: InputTopicConsumer = mockk()

        every { consumer.isStopped() } returns false

        val expiryChecker = AppHealthChecker(
            consumer,
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isUnhealthy() shouldBe false
    }

    @Test
    fun `Should not consider app as unhealthy if not within expiry window and app lifetime is exceeded`() {
        val windowStart = (LocalTime.now(ZoneOffset.UTC).hour + 6) % 24
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 1200L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val consumer: InputTopicConsumer = mockk()

        every { consumer.isStopped() } returns false

        val expiryChecker = AppHealthChecker(
            consumer,
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isUnhealthy() shouldBe false
    }

    @Test
    fun `Should not allow app lifetime shorter than window`() {
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 60

        shouldThrow<Exception> {
            AppHealthChecker(
                mockk(),
                expiryWindowMinutes = windowDurationMinutes,
                minimumExpiryDurationMinutes = minLifetimeMinutes
            )
        }
    }

    @Test
    fun `Should consider app as unhealthy if consumer is not running`() {
        val consumer: InputTopicConsumer = mockk()

        every { consumer.isStopped() } returns true

        val expiryChecker = AppHealthChecker(
            consumer
        )

        expiryChecker.isUnhealthy() shouldBe true
    }
}
