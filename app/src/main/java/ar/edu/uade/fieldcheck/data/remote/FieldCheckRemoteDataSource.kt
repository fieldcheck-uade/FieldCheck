package ar.edu.uade.fieldcheck.data.remote

import ar.edu.uade.fieldcheck.data.remote.api.FieldCheckApi
import ar.edu.uade.fieldcheck.data.remote.dto.InspectionDto
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente de datos remota: habla con la API y clasifica cada respuesta (RNF03, RNF04).
 *
 * Es la única clase que conoce Retrofit y los códigos HTTP. Ningún método lanza excepciones:
 * todo vuelve como [ApiResult], con un mensaje que se le puede mostrar al usuario.
 *
 * Cómo se clasifica:
 * - Sin red o timeout → [ApiResult.TransientError].
 * - 408, 429 y 5xx → [ApiResult.TransientError] (el servidor puede recuperarse).
 * - 401 / 403 → [ApiResult.PermanentError] "API key inválida".
 * - Cualquier otro 4xx → [ApiResult.PermanentError] con el motivo que mandó el servidor.
 */
@Singleton
class FieldCheckRemoteDataSource @Inject constructor(
    private val api: FieldCheckApi,
    private val json: Json,
) {

    suspend fun getTemplates(): ApiResult<List<TemplateDto>> {
        return safeCall { api.getTemplates() }
    }

    suspend fun sendInspection(inspection: InspectionDto): ApiResult<Unit> {
        return safeCall { api.sendInspection(inspection) }
    }

    /**
     * Sube una foto. [itemPosition] ya tiene que venir en el formato del servidor (empieza en 1).
     */
    suspend fun uploadEvidence(
        inspectionId: String,
        evidenceId: String,
        itemPosition: Int,
        capturedAt: Long,
        latitude: Double?,
        longitude: Double?,
        photo: File,
    ): ApiResult<Unit> {
        val photoBody = photo.asRequestBody(JPEG)
        val filePart = MultipartBody.Part.createFormData("file", photo.name, photoBody)

        return safeCall {
            api.uploadEvidence(
                inspectionId = inspectionId,
                file = filePart,
                evidenceId = textPart(evidenceId),
                itemPosition = textPart(itemPosition.toString()),
                capturedAt = textPart(capturedAt.toString()),
                latitude = latitude?.let { textPart(it.toString()) },
                longitude = longitude?.let { textPart(it.toString()) },
            )
        }
    }

    private fun textPart(value: String): RequestBody {
        return value.toRequestBody(TEXT)
    }

    /**
     * Ejecuta el pedido y convierte cualquier resultado en [ApiResult].
     *
     * La [CancellationException] se vuelve a lanzar: significa que WorkManager detuvo el trabajo
     * (por ejemplo, se perdió la red), no que la API falló.
     */
    private suspend fun <T> safeCall(call: suspend () -> Response<T>): ApiResult<T> {
        try {
            val response = call()
            if (response.isSuccessful) {
                @Suppress("UNCHECKED_CAST")
                val body = (response.body() ?: Unit) as T
                return ApiResult.Success(body)
            }
            return classifyHttpError(response)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            return ApiResult.TransientError("El servidor tardó demasiado en responder. Se va a reintentar solo.")
        } catch (exception: IOException) {
            return ApiResult.TransientError("No se pudo conectar con el servidor. Se va a reintentar solo.")
        } catch (exception: SerializationException) {
            return ApiResult.PermanentError("El servidor respondió algo que la app no entiende.")
        }
    }

    private fun <T> classifyHttpError(response: Response<T>): ApiResult<T> {
        val code = response.code()

        if (code == 401 || code == 403) {
            return ApiResult.PermanentError("El servidor rechazó la API key de la app. Revisá la configuración.")
        }

        val isTransient = code == 408 || code == 429 || code >= 500
        if (isTransient) {
            return ApiResult.TransientError("El servidor no está disponible (código $code). Se va a reintentar solo.")
        }

        val detail = readErrorDetail(response)
        if (detail == null) {
            return ApiResult.PermanentError("El servidor rechazó el envío (código $code).")
        }
        return ApiResult.PermanentError("El servidor rechazó el envío: $detail (código $code).")
    }

    /**
     * FastAPI manda los errores como `{"detail": "texto"}` o, si falló la validación,
     * `{"detail": [{"msg": "..."}, ...]}`. Se toma el primer mensaje legible.
     * Si el cuerpo no tiene ese formato, devuelve `null` y se muestra solo el código.
     */
    private fun readErrorDetail(response: Response<*>): String? {
        try {
            val rawBody = response.errorBody()?.string()
            if (rawBody.isNullOrBlank()) {
                return null
            }
            val root = json.parseToJsonElement(rawBody)
            if (root !is JsonObject) {
                return null
            }
            val detail = root["detail"]
            if (detail is JsonPrimitive && detail.isString) {
                return detail.content
            }
            if (detail is JsonArray && detail.isNotEmpty()) {
                val first = detail[0]
                if (first is JsonObject) {
                    return first["msg"]?.jsonPrimitive?.content
                }
            }
            return null
        } catch (exception: Exception) {
            return null
        }
    }

    private companion object {
        val JPEG = "image/jpeg".toMediaType()
        val TEXT = "text/plain".toMediaType()
    }
}
