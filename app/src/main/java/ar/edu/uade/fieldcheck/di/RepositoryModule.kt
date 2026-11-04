package ar.edu.uade.fieldcheck.di

import ar.edu.uade.fieldcheck.data.repository.InspectionRepositoryImpl
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Une cada interfaz de repositorio (Domain) con su implementación (Data).
 *
 * `@Binds` le dice a Hilt: "cuando alguien pida un InspectionRepository,
 * entregale un InspectionRepositoryImpl". Es el punto donde se concreta
 * la inversión de dependencias.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInspectionRepository(
        implementation: InspectionRepositoryImpl,
    ): InspectionRepository
}
