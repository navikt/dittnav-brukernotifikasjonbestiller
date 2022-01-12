package no.nav.personbruker.brukernotifikasjonbestiller.common.database

import com.zaxxer.hikari.HikariDataSource
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import org.flywaydb.core.Flyway

class LocalPostgresDatabase : Database {

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
            isAutoCommit = false
            validate()
        }
    }

    private fun flyway() {
        Flyway.configure()
                .connectRetries(3)
                .dataSource(dataSource)
                .load()
                .migrate()
    }
}
