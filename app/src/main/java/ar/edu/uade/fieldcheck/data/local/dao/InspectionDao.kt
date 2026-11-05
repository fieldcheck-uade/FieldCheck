package ar.edu.uade.fieldcheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a las tablas de inspecciones e ítems.
 *
 * Las lecturas devuelven [Flow]: Room vuelve a emitir automáticamente
 * cada vez que cambia alguna de las tablas consultadas.
 */
@Dao
interface InspectionDao {

    /**
     * Todas las inspecciones con su progreso.
     *
     * - `COUNT(item_results.id)` cuenta todos los ítems de la inspección.
     * - `COUNT(item_results.result)` cuenta solo los ítems con resultado,
     *   porque COUNT de una columna ignora los valores NULL.
     * - Se usa LEFT JOIN para que también aparezcan inspecciones sin ítems.
     */
    @Query(
        """
        SELECT inspections.*,
               COUNT(item_results.id) AS totalItems,
               COUNT(item_results.result) AS completedItems
        FROM inspections
        LEFT JOIN item_results ON item_results.inspectionId = inspections.clientUuid
        GROUP BY inspections.clientUuid
        """
    )
    fun observeAllWithProgress(): Flow<List<InspectionWithProgress>>

    @Query("SELECT COUNT(*) FROM inspections")
    suspend fun countInspections(): Int

    @Insert
    suspend fun insertInspection(inspection: InspectionEntity)

    @Insert
    suspend fun insertItemResults(itemResults: List<ItemResultEntity>)
}
