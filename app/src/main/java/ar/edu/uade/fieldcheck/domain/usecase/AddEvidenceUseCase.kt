package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: guardar una foto ya tomada como evidencia de un ítem.
 *
 * Reglas:
 * - La foto tiene que tener un archivo: sin ruta no hay nada que guardar.
 * - Antes de guardarla se reduce (RNF07): una foto de la cámara puede pesar varios MB y
 *   tardaría mucho en subir con señal débil. Si no se puede reducir, no se guarda.
 * - La fecha de captura es el momento en que la cámara devolvió la foto.
 * - La ubicación no se captura en esta versión (fuera de alcance), por eso no se recibe.
 */
class AddEvidenceUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val photoStorage: PhotoStorage,
    private val clock: Clock,
) {

    suspend operator fun invoke(itemResultId: Long, filePath: String): Resource<Unit> {
        if (filePath.isBlank()) {
            return Resource.Error("No se pudo guardar la foto. Intentá de nuevo.")
        }

        val capturedAt = clock.millis()

        val compressResult = photoStorage.compressPhoto(filePath)
        if (compressResult is Resource.Error) {
            return compressResult
        }

        return repository.addEvidence(
            itemResultId = itemResultId,
            filePath = filePath,
            capturedAt = capturedAt,
        )
    }
}
