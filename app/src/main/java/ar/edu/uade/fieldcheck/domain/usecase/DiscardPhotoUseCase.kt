package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import javax.inject.Inject

/**
 * Caso de uso: borrar el archivo preparado para una foto que no se llegó a tomar
 * (el usuario canceló la cámara). Así no quedan archivos vacíos en el teléfono.
 */
class DiscardPhotoUseCase @Inject constructor(
    private val photoStorage: PhotoStorage,
) {

    operator fun invoke(filePath: String) {
        photoStorage.deletePhotoFile(filePath)
    }
}
