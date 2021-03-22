package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.config


import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

object Flyway {

    fun runFlywayMigrations(env: Environment) {
        val flyway = configure(env).load()
        flyway.migrate()
    }

    private fun configure(env: Environment): FluentConfiguration {
        val configBuilder = Flyway.configure()
        val dataSource = createDataSourceForLocalDbWithUser(env)
        configBuilder.dataSource(dataSource)

         //TODO må vi gjøre dette i gcp?
      /*  if (isCurrentlyRunningOnNais()) {
            configBuilder.initSql("SET ROLE \"${env.dbUser}\"")
        }

       */

        return configBuilder
    }

    private fun createDataSourceForLocalDbWithUser(env: Environment): HikariDataSource {
        return PostgresDatabase.hikariFromLocalDb(env)
    }

}
