package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto

/**
 * Archivos de las fotos de evidencia en el almacenamiento interno del teléfono.
 *
 * Las fotos no van a la base de datos (agrandarían la base); la base guarda solo la ruta.
 */
interface PhotoStorage {

    /** Crea un archivo vacío donde la cámara va a guardar la próxima foto. */
    fun createPhotoFile(): Resource<PendingPhoto>

    /**
     * Borra el archivo de una foto, por ejemplo si el usuario canceló la cámara.
     *
     * @return `true` si el archivo existía y se borró.
     */
    fun deletePhotoFile(filePath: String): Boolean
}
