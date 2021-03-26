package no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database

import no.nav.personbruker.dittnav.brukernotifikasjonbestiller.common.database.exception.BatchUpdateException

class ListPersistActionResult<T> private constructor(private val resultList: List<RowResult<T>>) {
    companion object {
        fun <T> mapParamListToResultArray(paramEntities: List<T>, resultArray: IntArray): ListPersistActionResult<T> {
            if (paramEntities.size != resultArray.size) {
                throw BatchUpdateException("Lengde på batch update resultat samsvarer ikke med antall parametere.")
            }

            return paramEntities.mapIndexed { index, entity ->
                when (resultArray[index]) {
                    1 -> RowResult(entity, PersistFailureReason.NO_ERROR)
                    0 -> RowResult(entity, PersistFailureReason.CONFLICTING_KEYS)
                    else -> throw BatchUpdateException("Udefinert resultat etter batch update.")
                }
            }.let { resultList ->
                ListPersistActionResult(resultList)
            }
        }
    }
}

private data class RowResult<T>(val entity: T, val status: PersistFailureReason)
