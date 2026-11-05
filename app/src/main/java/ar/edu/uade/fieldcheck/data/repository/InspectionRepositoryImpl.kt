package ar.edu.uade.fieldcheck.data.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.entity.InspectionWithProgress
import ar.edu.uade.fieldcheck.data.mapper.InspectionMapper
import ar.edu.uade.fieldcheck.domain.model.Inspection
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de inspecciones usando Room.
 *
 * Cumple el contrato [InspectionRepository] definido en Domain. Hilt sabe que tiene que
 * entregar esta clase cuando alguien pide un `InspectionRepository` (ver `di/RepositoryModule`).
 */
@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val inspectionDao: InspectionDao,
) : InspectionRepository {

    override fun observeInspections(): Flow<Resource<List<Inspection>>> {
        val rowsFlow: Flow<List<InspectionWithProgress>> = inspectionDao.observeAllWithProgress()

        val successFlow: Flow<Resource<List<Inspection>>> = rowsFlow.map { rows ->
            val inspections = InspectionMapper.toDomainList(rows)
            Resource.Success(inspections)
        }

        // Si la base falla, no dejamos que la app se cierre: avisamos con un Resource.Error.
        return successFlow.catch { exception ->
            val error = Resource.Error(
                message = "No pudimos leer las inspecciones guardadas.",
                cause = exception,
            )
            emit(error)
        }
    }
}
