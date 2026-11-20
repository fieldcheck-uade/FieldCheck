package ar.edu.uade.fieldcheck.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de preferencias del usuario, definido en la capa de Domain.
 *
 * Igual que [InspectionRepository]: el dominio dice QUÉ necesita y Data decide CÓMO
 * guardarlo (hoy DataStore). Así los casos de uso se testean con un repositorio falso.
 */
interface SettingsRepository {

    /**
     * Observa si el modo oscuro está activado.
     *
     * Si el usuario nunca lo cambió, emite `false`: la app arranca en modo claro.
     */
    fun observeDarkMode(): Flow<Boolean>

    /** Guarda la elección del usuario para que se mantenga al cerrar y reabrir la app. */
    suspend fun setDarkMode(enabled: Boolean)
}
