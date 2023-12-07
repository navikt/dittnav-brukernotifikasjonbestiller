package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.kafka.InputTopicConsumer
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class AppHealthChecker (
    private val inputConsumer: InputTopicConsumer,
    private val expiryHourUTC: Int = 2,
    private val expiryWindowMinutes: Int = 60,
    private val minimumExpiryDurationMinutes: Int = 6 * 60,
    private val appStartupTime: Instant = Instant.now(),
) {
    init {
        require(expiryWindowMinutes < minimumExpiryDurationMinutes) {
            "'expiryWindowMinutes' må være mindre enn 'minimumExpiryDurationMinutes' for å forhindre unødig expiry."
        }
    }

    fun isUnhealthy(): Boolean {
        return inputConsumer.isStopped() || appHasExpired()
    }

    private fun appHasExpired(): Boolean {
        return appLifetimeExceedsExpiry() && isWithinExpiryWindow()
    }

    private fun isWithinExpiryWindow(): Boolean {
        val currentTimeUTC = LocalTime.now(ZoneId.of("Z"))

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
