package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.health

import java.time.*

class AppExpiryChecker (
    private val expiryHourUTC: Int = 2,
    private val expiryWindowMinutes: Int = 60,
    private val minimumExpiryDurationMinutes: Int = 6 * 60,
    private val appStartupTime: Instant = Instant.now()
) {
    init {
        require(expiryWindowMinutes < minimumExpiryDurationMinutes) {
            "'expiryWindowMinutes' må være mindre enn 'minimumExpiryDurationMinutes' for å forhindre unødig expiry."
        }
    }

    fun isExpired(): Boolean {
        return appLifetimeExceedsExpiry() && isWithinExpiryWindow()
    }

    private fun isWithinExpiryWindow(): Boolean {
        val currentTimeUTC = LocalDateTime.now(ZoneOffset.UTC).toLocalTime()

        val windowStart = LocalTime.of(expiryHourUTC, 0)
        val windowEnd = windowStart.plusMinutes(expiryWindowMinutes.toLong())

        return currentTimeUTC.isAfter(windowStart) && currentTimeUTC.isBefore(windowEnd)
    }

    private fun appLifetimeExceedsExpiry(): Boolean {
        return minutesSinceAppStartup() > minimumExpiryDurationMinutes
    }

    private fun minutesSinceAppStartup(): Int {
        val durationSinceStart = Duration.between(appStartupTime, Instant.now())

        return durationSinceStart.toMinutes().toInt()
    }
}