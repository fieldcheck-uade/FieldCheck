package ar.edu.uade.fieldcheck.domain.model

/**
 * Inspección tal como la usa la app (modelo de dominio).
 *
 * Es Kotlin puro: no sabe nada de Room, Retrofit ni Android.
 * Las fechas son milisegundos desde epoch para no depender de ninguna librería de fechas.
 */
data class Inspection(
    val clientUuid: String,
    val templateName: String,
    val siteName: String,
    val status: InspectionStatus,
    val syncStatus: SyncStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val totalItems: Int,
    val completedItems: Int,
) {

    /** Una inspección finalizada es de solo lectura y es la única que se sincroniza. */
    fun isFinished(): Boolean {
        return status == InspectionStatus.FINISHED
    }
}

/** Ciclo de vida de una inspección. */
enum class InspectionStatus {
    IN_PROGRESS,
    FINISHED,
}

/** Estado de sincronización con el servidor (patrón outbox, ver docs/diagramas.md punto 5). */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED,
}
