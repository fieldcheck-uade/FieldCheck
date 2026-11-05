package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import javax.inject.Inject

/**
 * Caso de uso: eliminar una foto de evidencia ("¿Eliminar esta foto?" en P3).
 *
 * El repositorio borra la fila y también el archivo, para no dejar fotos huérfanas
 * ocupando espacio en el teléfono.
 */
class DeleteEvidenceUseCase @Inject constructor(
    private val repository: InspectionRepository,
) {

    suspend operator fun invoke(evidenceId: String): Resource<Unit> {
        return repository.deleteEvidence(evidenceId)
    }
}
