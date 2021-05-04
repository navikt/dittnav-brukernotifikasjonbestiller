package no.nav.personbruker.brukernotifikasjonbestiller.common.database

import com.zaxxer.hikari.HikariDataSource
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer

class H2Database : Database {

    private val memDataSource: HikariDataSource
    private val container = TestPostgresqlContainer()

    init {
        container.start()
        memDataSource = createDataSource()
        flyway()
    }

    override val dataSource: HikariDataSource
        get() = memDataSource

    private fun createDataSource(): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            validate()
        }
    }

    private fun flyway() {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate()
    }
}
