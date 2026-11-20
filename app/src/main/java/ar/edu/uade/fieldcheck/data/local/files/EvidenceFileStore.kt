package ar.edu.uade.fieldcheck.data.local.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda las fotos de evidencia en `files/evidence/`, dentro del almacenamiento interno
 * de la app: ninguna otra app puede leerlas y se borran si se desinstala FieldCheck.
 *
 * La cámara del sistema es otra app, así que no puede escribir directo en esa carpeta.
 * Por eso se le pasa una dirección `content://` creada con [FileProvider], que le da
 * permiso de escritura solo sobre ese archivo (ver `res/xml/file_paths.xml`).
 */
@Singleton
class EvidenceFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoStorage {

    override fun createPhotoFile(): Resource<PendingPhoto> {
        try {
            val directory = getEvidenceDirectory()
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, "evidence_${UUID.randomUUID()}.jpg")
            file.createNewFile()

            val contentUri: Uri = FileProvider.getUriForFile(context, getAuthority(), file)
            val pendingPhoto = PendingPhoto(
                filePath = file.absolutePath,
                contentUri = contentUri.toString(),
            )
            return Resource.Success(pendingPhoto)
        } catch (exception: IOException) {
            return Resource.Error(
                message = "No se pudo guardar la foto. Liberá espacio e intentá de nuevo.",
                cause = exception,
            )
        }
    }

    /**
     * Solo borra archivos que estén dentro de la carpeta de evidencias. Es una protección:
     * una ruta mal armada nunca puede terminar borrando otro archivo de la app.
     */
    override fun deletePhotoFile(filePath: String): Boolean {
        val file = File(filePath)
        if (!isInsideEvidenceDirectory(file)) {
            return false
        }
        return file.delete()
    }

    private fun isInsideEvidenceDirectory(file: File): Boolean {
        val evidenceDirectoryPath = getEvidenceDirectory().canonicalPath + File.separator
        val filePath = file.canonicalPath
        return filePath.startsWith(evidenceDirectoryPath)
    }

    private fun getEvidenceDirectory(): File {
        return File(context.filesDir, EVIDENCE_DIRECTORY)
    }

    /** Tiene que coincidir con `android:authorities` del provider en el AndroidManifest. */
    private fun getAuthority(): String {
        return "${context.packageName}.fileprovider"
    }

    private companion object {
        const val EVIDENCE_DIRECTORY = "evidence"
    }
}
