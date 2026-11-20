package ar.edu.uade.fieldcheck.data.local.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun compressPhoto(filePath: String): Resource<Unit> {
        // Decodificar y comprimir una imagen es trabajo de CPU: fuera del hilo principal.
        return withContext(Dispatchers.Default) {
            try {
                compressInPlace(File(filePath))
                Resource.Success(Unit)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Resource.Error(
                    message = "No se pudo procesar la foto. Intentá sacarla de nuevo.",
                    cause = exception,
                )
            }
        }
    }

    /**
     * 1. Lee solo el tamaño, para decodificar ya submuestreada y no llenar la memoria.
     * 2. La achica hasta que el lado mayor mida 1600 px.
     * 3. La gira según el EXIF: la cámara suele guardar la foto "acostada" con una marca de
     *    orientación, y esa marca se pierde al volver a comprimir.
     * 4. Escribe en un archivo temporal y reemplaza el original, para no dejar nunca
     *    una foto a medio escribir.
     */
    private fun compressInPlace(file: File) {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("La foto está vacía o no es una imagen")
        }

        val options = BitmapFactory.Options()
        options.inSampleSize = PhotoResizeCalculator.sampleSize(bounds.outWidth, bounds.outHeight)
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("No se pudo leer la foto")

        val (targetWidth, targetHeight) = PhotoResizeCalculator.targetSize(decoded.width, decoded.height)
        val matrix = Matrix()
        matrix.postScale(
            targetWidth.toFloat() / decoded.width,
            targetHeight.toFloat() / decoded.height,
        )
        matrix.postRotate(readRotationDegrees(file))

        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val temporaryFile = File(file.parentFile, "${file.name}.tmp")
        try {
            temporaryFile.outputStream().use { output ->
                val written = result.compress(Bitmap.CompressFormat.JPEG, PhotoResizeCalculator.JPEG_QUALITY, output)
                if (!written) {
                    throw IOException("No se pudo comprimir la foto")
                }
            }
            if (!temporaryFile.renameTo(file)) {
                throw IOException("No se pudo reemplazar la foto")
            }
        } finally {
            temporaryFile.delete()
            if (result !== decoded) {
                result.recycle()
            }
            decoded.recycle()
        }
    }

    private fun readRotationDegrees(file: File): Float {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
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
