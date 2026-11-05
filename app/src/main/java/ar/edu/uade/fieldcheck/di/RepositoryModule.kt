package ar.edu.uade.fieldcheck.di

import ar.edu.uade.fieldcheck.data.connectivity.AndroidNetworkMonitor
import ar.edu.uade.fieldcheck.data.local.files.EvidenceFileStore
import ar.edu.uade.fieldcheck.data.repository.InspectionRepositoryImpl
import ar.edu.uade.fieldcheck.data.repository.TemplateRepositoryImpl
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import ar.edu.uade.fieldcheck.domain.repository.NetworkMonitor
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import ar.edu.uade.fieldcheck.domain.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Une cada interfaz del dominio con su implementación en Data.
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

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        implementation: TemplateRepositoryImpl,
    ): TemplateRepository

    /** Conexión real del teléfono; en los tests se reemplaza por `FakeNetworkMonitor`. */
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        implementation: AndroidNetworkMonitor,
    ): NetworkMonitor

    /** Archivos de las fotos en el almacenamiento interno. */
    @Binds
    @Singleton
    abstract fun bindPhotoStorage(
        implementation: EvidenceFileStore,
    ): PhotoStorage
}
