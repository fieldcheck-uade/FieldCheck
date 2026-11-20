package ar.edu.uade.fieldcheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithItems
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a las tablas de inspecciones e ítems.
 *
 * Las lecturas devuelven [Flow]: Room vuelve a emitir automáticamente
 * cada vez que cambia alguna de las tablas consultadas.
 *
 * Las actualizaciones devuelven la cantidad de filas cambiadas: si es 0, la fila no existía
 * (o no cumplía la condición) y el repositorio lo informa como error.
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

    /**
     * Una inspección con sus ítems y fotos. Emite `null` si la inspección no existe.
     *
     * `@Transaction` hace que Room lea la inspección y sus relaciones en una sola transacción,
     * así nunca se mezcla una inspección vieja con ítems nuevos.
     */
    @Transaction
    @Query("SELECT * FROM inspections WHERE clientUuid = :inspectionId")
    fun observeInspectionWithItems(inspectionId: String): Flow<InspectionWithItems?>

    @Insert
    suspend fun insertInspection(inspection: InspectionEntity)

    @Insert
    suspend fun insertItemResults(itemResults: List<ItemResultEntity>)

    /**
     * Guarda la inspección y sus ítems juntos: o se guarda todo o no se guarda nada.
     * Sin la transacción podría quedar una inspección sin ítems si la app se cierra a mitad.
     */
    @Transaction
    suspend fun insertInspectionWithItems(
        inspection: InspectionEntity,
        itemResults: List<ItemResultEntity>,
    ) {
        insertInspection(inspection)
        insertItemResults(itemResults)
    }

    @Query(
        """
        UPDATE item_results
        SET result = :result, note = :note, updatedAt = :updatedAt
        WHERE id = :itemResultId
        """
    )
    suspend fun updateItemResult(
        itemResultId: Long,
        result: String?,
        note: String?,
        updatedAt: Long,
    ): Int

    /** Solo cambia inspecciones "En curso": una finalizada es de solo lectura (R6). */
    @Query(
        """
        UPDATE inspections
        SET status = 'FINISHED',
            finishedAt = :finishedAt,
            syncStatus = 'PENDING',
            lastSyncError = NULL,
            updatedAt = :finishedAt
        WHERE clientUuid = :inspectionId AND status = 'IN_PROGRESS'
        """
    )
    suspend fun finishInspection(inspectionId: String, finishedAt: Long): Int

    @Query(
        """
        UPDATE inspections
        SET syncStatus = 'PENDING', lastSyncError = NULL
        WHERE clientUuid = :inspectionId
        """
    )
    suspend fun markForSyncRetry(inspectionId: String): Int

    // ---- Sincronización (RF03) ----

    /** Inspecciones finalizadas que faltan subir, de la más vieja a la más nueva. */
    @Query(
        """
        SELECT clientUuid FROM inspections
        WHERE status = 'FINISHED' AND syncStatus = 'PENDING'
        ORDER BY finishedAt
        """
    )
    suspend fun getPendingFinishedIds(): List<String>

    /** Lectura puntual (sin Flow) de una inspección con ítems y fotos, para armar el envío. */
    @Transaction
    @Query("SELECT * FROM inspections WHERE clientUuid = :inspectionId")
    suspend fun getInspectionWithItems(inspectionId: String): InspectionWithItems?

    @Query(
        """
        UPDATE inspections
        SET syncStatus = 'SYNCED', lastSyncError = NULL
        WHERE clientUuid = :inspectionId
        """
    )
    suspend fun markSynced(inspectionId: String): Int

    @Query(
        """
        UPDATE inspections
        SET syncStatus = 'FAILED', lastSyncError = :message
        WHERE clientUuid = :inspectionId
        """
    )
    suspend fun markFailed(inspectionId: String, message: String): Int
}
