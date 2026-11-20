package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.domain.model.SyncResult

/**
 * Contrato para subir al servidor las inspecciones finalizadas (RF03).
 *
 * El dominio decide QUÉ hacer con cada resultado (ver `SyncPendingInspectionsUseCase`);
 * la capa de Data sabe CÓMO hablar con la API y con Room.
 */
interface SyncRepository {

    /** Ids de las inspecciones finalizadas que todavía no se subieron (estado PENDING). */
    suspend fun getPendingInspectionIds(): List<String>

    /**
     * Sube una inspección: primero los datos y después las fotos que falten.
     *
     * Es seguro llamarlo varias veces: el servidor identifica todo por `clientUuid`,
     * así que un reintento nunca duplica la inspección ni sus fotos (RNF03).
     */
    suspend fun uploadInspection(inspectionId: String): SyncResult

    /** La inspección quedó completa en el servidor: pasa a SYNCED y se borra el error. */
    suspend fun markSynced(inspectionId: String)

    /** El servidor la rechazó: pasa a FAILED y se guarda [message] para mostrarlo en el detalle. */
    suspend fun markFailed(inspectionId: String, message: String)
}
