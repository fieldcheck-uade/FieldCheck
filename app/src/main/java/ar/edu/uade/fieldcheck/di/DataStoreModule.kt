package ar.edu.uade.fieldcheck.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Le explica a Hilt cómo crear el DataStore de preferencias.
 *
 * Tiene que haber una sola instancia por archivo en toda la app (si no, DataStore falla),
 * por eso va en `SingletonComponent` con `@Singleton`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val SETTINGS_FILE_NAME = "settings"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE_NAME) },
        )
    }
}
