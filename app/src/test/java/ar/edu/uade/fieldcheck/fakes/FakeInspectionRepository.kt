package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.model.InspectionStatus
import ar.edu.uade.fieldcheck.domain.model.SyncStatus
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio falso para tests: reemplaza a Room.
 *
 * Es posible gracias a que el dominio depende de la interfaz [InspectionRepository]
 * y no de la implementación concreta. Desde el test se cambia lo que "hay guardado"
 * llamando a [emit].
 */
class FakeInspectionRepository(
    initialValue: Resource<List<Inspection>> = Resource.Success(emptyList()),
) : InspectionRepository {

    private val inspections = MutableStateFlow(initialValue)

    override fun observeInspections(): Flow<Resource<List<Inspection>>> {
        return inspections
    }

    fun emit(value: Resource<List<Inspection>>) {
        inspections.value = value
    }
}

/** Crea una inspección de prueba con valores por defecto; cada test cambia solo lo que le importa. */
fun createTestInspection(
    clientUuid: String,
    status: InspectionStatus = InspectionStatus.IN_PROGRESS,
    startedAt: Long = 1_000L,
    finishedAt: Long? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING,
): Inspection {
    return Inspection(
        clientUuid = clientUuid,
        templateName = "Seguridad eléctrica",
        siteName = "Sitio $clientUuid",
        status = status,
        syncStatus = syncStatus,
        startedAt = startedAt,
        finishedAt = finishedAt,
        totalItems = 5,
        completedItems = 2,
    )
}
