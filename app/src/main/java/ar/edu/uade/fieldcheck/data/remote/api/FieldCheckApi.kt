package ar.edu.uade.fieldcheck.data.remote.api

import ar.edu.uade.fieldcheck.data.remote.dto.InspectionDto
import ar.edu.uade.fieldcheck.data.remote.dto.TemplateDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Endpoints de la API de FieldCheck (ver `backend/README.md`). Retrofit genera la implementación.
 *
 * Todos devuelven [Response] en lugar del cuerpo directo: así un 4xx o un 5xx no se convierte en
 * excepción y `FieldCheckRemoteDataSource` puede leer el código para decidir si se reintenta.
 * La API key la agrega `ApiKeyInterceptor` a todos los pedidos.
 */
interface FieldCheckApi {

    @GET("templates")
    suspend fun getTemplates(): Response<List<TemplateDto>>

    /** Idempotente por `clientUuid`: responde 201 si la creó y 200 si ya existía. */
    @POST("inspections")
    suspend fun sendInspection(@Body inspection: InspectionDto): Response<Unit>

    /**
     * Sube una foto de un ítem. Idempotente por el `clientUuid` de la foto.
     * Las coordenadas son opcionales: si son `null`, Retrofit no manda esa parte.
     */
    @Multipart
    @POST("inspections/{inspectionId}/evidence")
    suspend fun uploadEvidence(
        @Path("inspectionId") inspectionId: String,
        @Part file: MultipartBody.Part,
        @Part("clientUuid") evidenceId: RequestBody,
        @Part("itemPosition") itemPosition: RequestBody,
        @Part("capturedAt") capturedAt: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
    ): Response<Unit>
}
