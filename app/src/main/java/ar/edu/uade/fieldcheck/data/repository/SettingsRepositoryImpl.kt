package ar.edu.uade.fieldcheck.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import ar.edu.uade.fieldcheck.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de preferencias usando DataStore.
 *
 * Se usa DataStore y no Room porque son pocos valores sueltos (clave → valor), no tablas.
 * Hilt la entrega cuando alguien pide un `SettingsRepository` (ver `di/RepositoryModule`).
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeDarkMode(): Flow<Boolean> {
        // Si el archivo no se puede leer, seguimos con los valores por defecto (modo claro)
        // en vez de cerrar la app. Cualquier otro error no lo esperamos y se propaga.
        val safePreferences = dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

        return safePreferences.map { preferences ->
            preferences[DARK_MODE_KEY] ?: DEFAULT_DARK_MODE
        }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    private companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

        /** Por defecto la app arranca en modo claro. */
        const val DEFAULT_DARK_MODE = false
    }
}
