package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Embedded

/**
 * Resultado de la consulta del historial: la inspección más el conteo de sus ítems.
 *
 * No es una tabla. Room arma este objeto a partir de las columnas que devuelve
 * `InspectionDao.observeAllWithProgress()`.
 */
data class InspectionWithProgress(
    @Embedded
    val inspection: InspectionEntity,
    val totalItems: Int,
    val completedItems: Int,
)
