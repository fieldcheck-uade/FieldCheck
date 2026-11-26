package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto

/**
 * Archivos de las fotos de evidencia en el almacenamiento interno del teléfono.
 *
 * Las fotos no van a la base de datos (agrandarían la base); la base guarda solo la ruta.
 */
interface PhotoStorage {

    /**
     * Crea un archivo vacío donde la cámara va a guardar la próxima foto.
     * Es `suspend` porque toca el disco: la implementación lo hace fuera del hilo principal.
     */
    suspend fun createPhotoFile(): Resource<PendingPhoto>

    /**
     * Borra el archivo de una foto, por ejemplo si el usuario canceló la cámara.
     * Es `suspend` porque toca el disco: la implementación lo hace fuera del hilo principal.
     *
     * @return `true` si el archivo existía y se borró.
     */
    suspend fun deletePhotoFile(filePath: String): Boolean

    /**
     * Reduce la foto ya tomada en el mismo archivo (RNF07): lado mayor de 1600 px y JPEG
     * calidad 80, para que pese alrededor de 1 MB o menos y se pueda subir con señal débil.
     * Respeta la orientación con la que se sacó la foto.
     */
    suspend fun compressPhoto(filePath: String): Resource<Unit>
}
