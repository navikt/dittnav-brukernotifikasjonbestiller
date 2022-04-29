package no.nav.personbruker.brukernotifikasjonbestiller.common.database

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.Database
import org.flywaydb.core.Flyway

class LocalPostgresDatabase private constructor(): Database {

    private val memDataSource: HikariDataSource
    private val container = TestPostgresqlContainer()

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate()
            }
        }

        fun cleanDb(): LocalPostgresDatabase {
            runBlocking {
                instance.dbQuery {
                    prepareStatement("delete from brukernotifikasjonbestilling").execute()
                }
            }
            return instance
        }
    }

    init {
        container.start()
        memDataSource = createDataSource()
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

    private fun migrate() {
        Flyway.configure()
                .connectRetries(3)
                .dataSource(dataSource)
                .load()
                .migrate()
    }
}
