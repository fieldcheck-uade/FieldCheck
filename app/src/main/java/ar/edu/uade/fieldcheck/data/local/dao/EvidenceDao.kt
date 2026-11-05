package ar.edu.uade.fieldcheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ar.edu.uade.fieldcheck.data.local.entity.EvidenceEntity

/**
 * Acceso a la tabla de fotos de evidencia.
 *
 * Las fotos se leen junto con su ítem (ver `InspectionDao.observeInspectionWithItems`);
 * acá quedan solo las escrituras y la búsqueda puntual para borrar el archivo.
 */
@Dao
interface EvidenceDao {

    @Insert
    suspend fun insertEvidence(evidence: EvidenceEntity)

    @Query("SELECT * FROM evidence WHERE clientUuid = :evidenceId")
    suspend fun getEvidence(evidenceId: String): EvidenceEntity?

    @Query("DELETE FROM evidence WHERE clientUuid = :evidenceId")
    suspend fun deleteEvidence(evidenceId: String): Int
}
