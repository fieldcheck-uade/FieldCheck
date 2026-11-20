package ar.edu.uade.fieldcheck.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega el header `X-API-Key` a todos los pedidos, así ningún endpoint se olvida de mandarlo.
 *
 * La clave llega desde `BuildConfig.API_KEY`, que se arma con `local.properties`: no está
 * escrita en el código (RNF08).
 */
class ApiKeyInterceptor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(HEADER_NAME, apiKey)
            .build()
        return chain.proceed(request)
    }

    companion object {
        const val HEADER_NAME = "X-API-Key"
    }
}
