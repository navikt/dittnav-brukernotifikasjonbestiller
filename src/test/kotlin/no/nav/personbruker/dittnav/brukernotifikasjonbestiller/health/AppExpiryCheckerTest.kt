package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
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

        expiryChecker.isExpired() `should be equal to` true
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

        expiryChecker.isExpired() `should be equal to` false
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

        expiryChecker.isExpired() `should be equal to` false
    }

    @Test
    fun `Should not allow app lifetime shorter than window`() {
        val windowDurationMinutes = 120
        val minLifetimeMinutes = 60

        invoking {
            AppExpiryChecker(
                expiryWindowMinutes = windowDurationMinutes,
                minimumExpiryDurationMinutes = minLifetimeMinutes
            )
        } `should throw` Exception::class
    }
}
