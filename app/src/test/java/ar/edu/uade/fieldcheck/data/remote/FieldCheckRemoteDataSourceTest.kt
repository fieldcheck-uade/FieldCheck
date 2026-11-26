package ar.edu.uade.fieldcheck.data.remote

import ar.edu.uade.fieldcheck.data.remote.dto.InspectionDto
import ar.edu.uade.fieldcheck.data.remote.dto.ItemResultDto
import ar.edu.uade.fieldcheck.di.NetworkModule
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

/**
 * Prueba el cliente de la API contra un servidor HTTP falso (MockWebServer), sin emulador.
 * Verifica lo que pide RNF03: qué errores se reintentan solos y cuáles no.
 */
class FieldCheckRemoteDataSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var dataSource: FieldCheckRemoteDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val json = NetworkModule.createJson()
        val client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInterceptor("test-key"))
            .build()
        val api = NetworkModule.createApi(server.url("/").toString(), client, json)
        dataSource = FieldCheckRemoteDataSource(api, json)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `sending an inspection posts camelCase json with the api key`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body("{}").build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertEquals(ApiResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/inspections", request.url.encodedPath)
        assertEquals("test-key", request.headers[ApiKeyInterceptor.HEADER_NAME])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"clientUuid\":\"uuid-1\""))
        assertTrue(body.contains("\"templateVersion\":3"))
        assertTrue(body.contains("\"position\":1"))
    }

    @Test
    fun `an inspection that already existed (200) also counts as success`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())

        assertEquals(ApiResult.Success(Unit), dataSource.sendInspection(createInspectionDto()))
    }

    @Test
    fun `server errors are transient`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertTrue(result is ApiResult.TransientError)
    }

    @Test
    fun `a missing server is transient`() = runTest {
        server.close()

        val result = dataSource.sendInspection(createInspectionDto())

        assertTrue(result is ApiResult.TransientError)
    }

    @Test
    fun `a timeout is transient`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).headersDelay(3, TimeUnit.SECONDS).build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertTrue(result is ApiResult.TransientError)
    }

    @Test
    fun `an invalid api key is permanent`() = runTest {
        server.enqueue(MockResponse.Builder().code(401).body("""{"detail":"API key inválida o ausente"}""").build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertEquals(
            ApiResult.PermanentError("El servidor rechazó la API key de la app. Revisá la configuración."),
            result,
        )
    }

    @Test
    fun `a validation error is permanent and shows the server reason`() = runTest {
        val body = """{"detail":[{"msg":"finishedAt no puede ser anterior a startedAt"}]}"""
        server.enqueue(MockResponse.Builder().code(422).body(body).build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertEquals(
            ApiResult.PermanentError(
                "El servidor rechazó el envío: finishedAt no puede ser anterior a startedAt (código 422).",
            ),
            result,
        )
    }

    @Test
    fun `a photo is uploaded as multipart with its fields`() = runTest {
        server.enqueue(MockResponse.Builder().code(201).body("{}").build())
        val photo = temporaryFolder.newFile("foto.jpg")
        photo.writeBytes(byteArrayOf(1, 2, 3))

        val result = dataSource.uploadEvidence(
            inspectionId = "uuid-1",
            evidenceId = "photo-1",
            itemPosition = 2,
            capturedAt = 1_500L,
            latitude = null,
            longitude = null,
            photo = photo,
        )

        assertEquals(ApiResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("/inspections/uuid-1/evidence", request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"file\"; filename=\"foto.jpg\""))
        assertTrue(body.contains("name=\"clientUuid\""))
        assertTrue(body.contains("photo-1"))
        assertTrue(body.contains("name=\"itemPosition\""))
        assertTrue(!body.contains("name=\"latitude\""))
    }

    @Test
    fun `templates are downloaded and parsed ignoring unknown fields`() = runTest {
        val body = """
            [{"id":"t1","name":"Sala técnica","version":2,"extra":"x",
              "items":[{"id":"t1-1","text":"Extintor","position":1}]}]
        """.trimIndent()
        server.enqueue(MockResponse.Builder().code(200).body(body).build())

        val result = dataSource.getTemplates()

        assertTrue(result is ApiResult.Success)
        val templates = (result as ApiResult.Success).data
        assertEquals("Sala técnica", templates[0].name)
        assertEquals(1, templates[0].items[0].position)
    }

    @Test
    fun `templates without a body (204) are a permanent error instead of a crash`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        val result = dataSource.getTemplates()

        assertEquals(ApiResult.PermanentError("El servidor respondió sin datos."), result)
    }

    @Test
    fun `an inspection answered without a body (204) still counts as success`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        val result = dataSource.sendInspection(createInspectionDto())

        assertEquals(ApiResult.Success(Unit), result)
    }

    private fun createInspectionDto(): InspectionDto {
        return InspectionDto(
            clientUuid = "uuid-1",
            templateName = "Control semanal de obra",
            templateVersion = 3,
            siteName = "Obra Belgrano",
            status = "FINISHED",
            startedAt = 1_000L,
            finishedAt = 2_000L,
            updatedAt = 2_000L,
            items = listOf(ItemResultDto(position = 1, itemText = "Vallado", result = "COMPLIES")),
        )
    }
}
