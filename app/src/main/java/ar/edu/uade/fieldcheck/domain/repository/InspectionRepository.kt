package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.Template
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de inspecciones, definido en la capa de Domain.
 *
 * Principio de inversión de dependencias (la "D" de SOLID): el dominio dice QUÉ necesita
 * y la capa de Data decide CÓMO conseguirlo (hoy Room, mañana también la API).
 * Gracias a esto los casos de uso se testean con un repositorio falso, sin emulador.
 *
 * Las fechas (`now`) llegan como parámetro: el repositorio no decide la hora,
 * así los tests pueden usar una fecha fija.
 */
interface InspectionRepository {

    /**
     * Observa todas las inspecciones guardadas en el teléfono.
     *
     * Devuelve un [Flow]: cada vez que cambia la base local se emite la lista nueva,
     * así la pantalla se actualiza sola.
     */
    fun observeInspections(): Flow<Resource<List<Inspection>>>

    /**
     * Observa una inspección con todos sus ítems (ordenados por posición) y sus fotos.
     * Emite de nuevo cada vez que se guarda un resultado, una nota o una foto.
     */
    fun observeInspectionDetail(inspectionId: String): Flow<Resource<InspectionDetail>>

    /**
     * Crea una inspección "En curso" a partir de una plantilla (regla R1).
     * Copia el nombre de la plantilla y el texto de cada ítem, todos sin resultado.
     *
     * @return el `clientUuid` de la inspección nueva.
     */
    suspend fun createInspection(template: Template, siteName: String, now: Long): Resource<String>

    /** Guarda el resultado y la nota de un ítem (regla R3: se guarda cada cambio al instante). */
    suspend fun saveItemResult(
        itemResultId: Long,
        result: ItemResultValue?,
        note: String?,
        now: Long,
    ): Resource<Unit>

    /** Agrega una foto, ya guardada en [filePath], como evidencia de un ítem. */
    suspend fun addEvidence(itemResultId: Long, filePath: String, capturedAt: Long): Resource<Unit>

    /** Borra una foto de evidencia: la fila de la base y el archivo. */
    suspend fun deleteEvidence(evidenceId: String): Resource<Unit>

    /** Pasa la inspección a FINISHED con [now] como fecha de fin y la deja PENDING para subir. */
    suspend fun finalizeInspection(inspectionId: String, now: Long): Resource<Unit>

    /** Vuelve a encolar la sincronización: la inspección pasa a PENDING y se borra el error. */
    suspend fun markForSyncRetry(inspectionId: String): Resource<Unit>
}
