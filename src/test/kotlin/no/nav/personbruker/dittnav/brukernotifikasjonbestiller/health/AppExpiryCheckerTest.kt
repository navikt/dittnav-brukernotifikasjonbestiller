package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

internal class AppExpiryCheckerTest {

    @Test
    fun `Should consider app expired if within expiry window and app lifetime is exceeded`() {
        val windowStart = LocalTime.now(ZoneOffset.UTC).hour
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 1200L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val expiryChecker = AppExpiryChecker(
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isExpired() shouldBe true
    }

    @Test
    fun `Should not consider app expired if within expiry window and app lifetime is not exceeded`() {
        val windowStart = LocalTime.now(ZoneOffset.UTC).hour
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 300L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val expiryChecker = AppExpiryChecker(
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isExpired() shouldBe false
    }

    @Test
    fun `Should not consider app exired if not within expiry window and app lifetime is exceeded`() {
        val windowStart = (LocalTime.now(ZoneOffset.UTC).hour + 6) % 24
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 600
        val appStartOffset = 1200L
        val appStart = Instant.now().minus(appStartOffset, ChronoUnit.MINUTES)

        val expiryChecker = AppExpiryChecker(
            expiryHourUTC = windowStart,
            expiryWindowMinutes = windowDurationMinutes,
            minimumExpiryDurationMinutes = minLifetimeMinutes,
            appStartupTime = appStart,
        )

        expiryChecker.isExpired() shouldBe false
    }

    @Test
    fun `Should not allow app lifetime shorter than window`() {
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 60

        shouldThrow<Exception> {
            AppExpiryChecker(
                expiryWindowMinutes = windowDurationMinutes,
                minimumExpiryDurationMinutes = minLifetimeMinutes
            )
        }
    }
}
