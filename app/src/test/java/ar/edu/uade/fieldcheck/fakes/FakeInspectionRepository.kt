package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Evidence
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionDetail
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.ItemResult
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.model.Template
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositorio falso para tests: reemplaza a Room.
 *
 * Es posible gracias a que el dominio depende de la interfaz [InspectionRepository]
 * y no de la implementación concreta.
 *
 * - Lo que "hay guardado" se cambia con [emit] (historial) y [emitDetail] (una inspección).
 * - Cada escritura queda anotada en una lista pública (por ejemplo [savedItemResults])
 *   para que el test verifique qué se pidió guardar.
 * - Las escrituras también actualizan el detalle guardado, como haría Room, así un
 *   ViewModel que observa el detalle ve el cambio.
 * - [writeResult] permite simular que la base falla en las escrituras.
 */
class FakeInspectionRepository(
    initialValue: Resource<List<Inspection>> = Resource.Success(emptyList()),
) : InspectionRepository {

    private val inspections = MutableStateFlow(initialValue)
    private val details = MutableStateFlow<Map<String, Resource<InspectionDetail>>>(emptyMap())

    /** Resultado de todas las escrituras que devuelven `Resource<Unit>`. */
    var writeResult: Resource<Unit> = Resource.Success(Unit)

    /** Resultado de [createInspection]. */
    var createInspectionResult: Resource<String> = Resource.Success("new-inspection-id")

    val createdInspections = mutableListOf<CreatedInspection>()
    val savedItemResults = mutableListOf<SavedItemResult>()
    val addedEvidence = mutableListOf<AddedEvidence>()
    val deletedEvidenceIds = mutableListOf<String>()
    val finalizedInspections = mutableListOf<FinalizedInspection>()
    val retriedInspectionIds = mutableListOf<String>()

    override fun observeInspections(): Flow<Resource<List<Inspection>>> {
        return inspections
    }

    fun emit(value: Resource<List<Inspection>>) {
        inspections.value = value
    }

    override fun observeInspectionDetail(inspectionId: String): Flow<Resource<InspectionDetail>> {
        return details.map { savedDetails ->
            val detail = savedDetails[inspectionId]
            if (detail == null) {
                Resource.Error("No encontramos esta inspección.")
            } else {
                detail
            }
        }
    }

    /** Guarda (o reemplaza) el detalle de una inspección; los observadores reciben el valor nuevo. */
    fun emitDetail(inspectionId: String, value: Resource<InspectionDetail>) {
        val updated = details.value.toMutableMap()
        updated[inspectionId] = value
        details.value = updated
    }

    override suspend fun createInspection(template: Template, siteName: String, now: Long): Resource<String> {
        createdInspections.add(CreatedInspection(template = template, siteName = siteName, now = now))
        return createInspectionResult
    }

    override suspend fun saveItemResult(
        itemResultId: Long,
        result: ItemResultValue?,
        note: String?,
        now: Long,
    ): Resource<Unit> {
        savedItemResults.add(SavedItemResult(itemResultId = itemResultId, result = result, note = note, now = now))
        if (writeResult is Resource.Success) {
            updateItem(itemResultId) { item -> item.copy(result = result, note = note) }
        }
        return writeResult
    }

    override suspend fun addEvidence(itemResultId: Long, filePath: String, capturedAt: Long): Resource<Unit> {
        addedEvidence.add(AddedEvidence(itemResultId = itemResultId, filePath = filePath, capturedAt = capturedAt))
        if (writeResult is Resource.Success) {
            val newEvidence = Evidence(
                clientUuid = "evidence-${addedEvidence.size}",
                filePath = filePath,
                capturedAt = capturedAt,
                latitude = null,
                longitude = null,
            )
            updateItem(itemResultId) { item -> item.copy(evidence = item.evidence + newEvidence) }
        }
        return writeResult
    }

    override suspend fun deleteEvidence(evidenceId: String): Resource<Unit> {
        deletedEvidenceIds.add(evidenceId)
        return writeResult
    }

    override suspend fun finalizeInspection(inspectionId: String, now: Long): Resource<Unit> {
        finalizedInspections.add(FinalizedInspection(inspectionId = inspectionId, now = now))
        if (writeResult is Resource.Success) {
            updateInspection(inspectionId) { inspection ->
                inspection.copy(
                    status = InspectionStatus.FINISHED,
                    finishedAt = now,
                    syncStatus = SyncStatus.PENDING,
                )
            }
        }
        return writeResult
    }

    override suspend fun markForSyncRetry(inspectionId: String): Resource<Unit> {
        retriedInspectionIds.add(inspectionId)
        if (writeResult is Resource.Success) {
            updateInspection(inspectionId) { inspection -> inspection.copy(syncStatus = SyncStatus.PENDING) }
        }
        return writeResult
    }

    /** Aplica [change] al ítem con ese id dentro del detalle guardado que lo contenga. */
    private fun updateItem(itemResultId: Long, change: (ItemResult) -> ItemResult) {
        val updated = details.value.toMutableMap()
        for ((inspectionId, resource) in details.value) {
            if (resource is Resource.Success) {
                val newItems = mutableListOf<ItemResult>()
                for (item in resource.data.items) {
                    if (item.id == itemResultId) {
                        newItems.add(change(item))
                    } else {
                        newItems.add(item)
                    }
                }
                updated[inspectionId] = Resource.Success(resource.data.copy(items = newItems))
            }
        }
        details.value = updated
    }

    private fun updateInspection(inspectionId: String, change: (Inspection) -> Inspection) {
        val resource = details.value[inspectionId]
        if (resource is Resource.Success) {
            val newDetail = resource.data.copy(inspection = change(resource.data.inspection))
            emitDetail(inspectionId, Resource.Success(newDetail))
        }
    }
}

data class CreatedInspection(val template: Template, val siteName: String, val now: Long)

data class SavedItemResult(val itemResultId: Long, val result: ItemResultValue?, val note: String?, val now: Long)

data class AddedEvidence(val itemResultId: Long, val filePath: String, val capturedAt: Long)

data class FinalizedInspection(val inspectionId: String, val now: Long)

/** Crea una inspección de prueba con valores por defecto; cada test cambia solo lo que le importa. */
fun createTestInspection(
    clientUuid: String,
    status: InspectionStatus = InspectionStatus.IN_PROGRESS,
    startedAt: Long = 1_000L,
    finishedAt: Long? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING,
    siteName: String = "Sitio $clientUuid",
    templateName: String = "Seguridad eléctrica",
): Inspection {
    return Inspection(
        clientUuid = clientUuid,
        templateName = templateName,
        siteName = siteName,
        status = status,
        syncStatus = syncStatus,
        startedAt = startedAt,
        finishedAt = finishedAt,
        totalItems = 5,
        completedItems = 2,
    )
}

/** Crea un ítem de prueba. El id por defecto es la posición + 1. */
fun createTestItemResult(
    position: Int,
    result: ItemResultValue? = null,
    note: String? = null,
    evidence: List<Evidence> = emptyList(),
    id: Long = position.toLong() + 1L,
    itemText: String = "Ítem ${position + 1}",
): ItemResult {
    return ItemResult(
        id = id,
        position = position,
        itemText = itemText,
        result = result,
        note = note,
        evidence = evidence,
    )
}

/** Crea una foto de evidencia de prueba, sin ubicación salvo que se indique. */
fun createTestEvidence(
    clientUuid: String = "evidence-1",
    filePath: String = "/files/evidence/photo.jpg",
    latitude: Double? = null,
    longitude: Double? = null,
): Evidence {
    return Evidence(
        clientUuid = clientUuid,
        filePath = filePath,
        capturedAt = 1_000L,
        latitude = latitude,
        longitude = longitude,
    )
}

/** Arma el detalle de una inspección con los ítems indicados. */
fun createTestDetail(
    items: List<ItemResult>,
    inspection: Inspection = createTestInspection("inspection-1"),
    lastSyncError: String? = null,
): InspectionDetail {
    return InspectionDetail(
        inspection = inspection,
        items = items,
        lastSyncError = lastSyncError,
    )
}
