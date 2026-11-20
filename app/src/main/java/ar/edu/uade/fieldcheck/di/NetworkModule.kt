package ar.edu.uade.fieldcheck.di

import ar.edu.uade.fieldcheck.BuildConfig
import ar.edu.uade.fieldcheck.data.remote.ApiKeyInterceptor
import ar.edu.uade.fieldcheck.data.remote.api.FieldCheckApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Le explica a Hilt cómo crear el cliente de la API (Retrofit + OkHttp + kotlinx.serialization).
 *
 * La URL y la API key salen de `BuildConfig`, que se arma con `local.properties` (RNF08).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return createJson()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            // Tiempos pensados para señal débil: las fotos pesan alrededor de 1 MB (RNF07).
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(ApiKeyInterceptor(BuildConfig.API_KEY))

        // El log de red existe solo en debug (RNF08), sin cuerpos ni la API key.
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BASIC
            logging.redactHeader(ApiKeyInterceptor.HEADER_NAME)
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideFieldCheckApi(client: OkHttpClient, json: Json): FieldCheckApi {
        return createApi(BuildConfig.API_BASE_URL, client, json)
    }

    /** JSON tolerante: si el servidor suma campos nuevos, la app no se rompe. */
    fun createJson(): Json {
        return Json {
            ignoreUnknownKeys = true
        }
    }

    /** Separado del `@Provides` para que los tests armen la API contra un servidor falso. */
    fun createApi(baseUrl: String, client: OkHttpClient, json: Json): FieldCheckApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FieldCheckApi::class.java)
    }
}
