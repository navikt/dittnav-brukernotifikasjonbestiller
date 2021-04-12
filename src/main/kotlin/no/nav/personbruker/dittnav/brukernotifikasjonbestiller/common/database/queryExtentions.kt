package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDateTime

fun Connection.executeBatchPersistQuery(sql: String, skipConflicting: Boolean = true, paramInit: PreparedStatement.() -> Unit): IntArray {
    autoCommit = false

    val finalSqlString = appendSkipStatementIfRequested(sql, skipConflicting)

    val result = prepareStatement(finalSqlString).use { statement ->
        statement.paramInit()
        statement.executeBatch()
    }
    commit()
    return result
}

fun <T> ResultSet.mapList(result: ResultSet.() -> T): List<T> =
        mutableListOf<T>().apply {
            while (next()) {
                add(result())
            }
        }

fun ResultSet.getUtcDateTime(columnLabel: String): LocalDateTime = getTimestamp(columnLabel).toLocalDateTime()

fun <T> IntArray.toBatchPersistResult(paramList: List<T>) = ListPersistActionResult.mapParamListToResultArray(paramList, this)

private fun appendSkipStatementIfRequested(baseSql: String, skipConflicting: Boolean) =
        if (skipConflicting) {
            """$baseSql ON CONFLICT DO NOTHING"""
        } else {
            baseSql
        }