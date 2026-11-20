package ar.edu.uade.fieldcheck.domain.model

/**
 * Archivo vacío preparado para que la cámara del sistema guarde una foto.
 *
 * @property filePath ruta del archivo en el almacenamiento interno; es lo que se guarda en la base.
 * @property contentUri dirección `content://` que se le pasa a la cámara. Es un String para que
 *   el dominio no dependa de `android.net.Uri`; la pantalla la convierte con `Uri.parse(...)`.
 */
data class PendingPhoto(
    val filePath: String,
    val contentUri: String,
)
