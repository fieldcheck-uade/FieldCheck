package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import javax.inject.Inject

/**
 * Caso de uso: preparar el archivo donde la cámara del sistema va a guardar la foto.
 *
 * La cámara necesita saber DÓNDE escribir antes de abrirse; por eso el archivo se crea
 * primero y, si el usuario cancela, se descarta con [DiscardPhotoUseCase].
 */
class PreparePhotoCaptureUseCase @Inject constructor(
    private val photoStorage: PhotoStorage,
) {

    operator fun invoke(): Resource<PendingPhoto> {
        return photoStorage.createPhotoFile()
    }
}
