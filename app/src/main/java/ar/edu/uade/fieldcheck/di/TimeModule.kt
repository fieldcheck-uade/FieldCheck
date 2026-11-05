package ar.edu.uade.fieldcheck.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Entrega el reloj de la app.
 *
 * Los casos de uso piden la hora a un [Clock] en lugar de llamar a `System.currentTimeMillis()`:
 * así, en los tests se usa un reloj fijo y las fechas guardadas son predecibles.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    fun provideClock(): Clock {
        return Clock.systemDefaultZone()
    }
}
