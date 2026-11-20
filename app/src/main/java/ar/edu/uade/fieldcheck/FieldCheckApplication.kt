package ar.edu.uade.fieldcheck

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import ar.edu.uade.fieldcheck.data.local.database.TemplateSeeder
import ar.edu.uade.fieldcheck.domain.repository.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de la app.
 *
 * `@HiltAndroidApp` arranca Hilt: a partir de acá Hilt puede crear e inyectar
 * dependencias en Activities, ViewModels y Workers.
 *
 * Implementa [Configuration.Provider] para que WorkManager cree los workers con la fábrica
 * de Hilt (así `SyncWorker` recibe sus casos de uso). Por eso en el manifest se saca el
 * inicializador automático de WorkManager.
 */
@HiltAndroidApp
class FieldCheckApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var templateSeeder: TemplateSeeder

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    /** Scope que vive lo mismo que la app; se usa para trabajo corto al iniciar. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            // La primera vez, sin haber tenido conexión nunca, la app arranca con plantillas de
            // ejemplo. Apenas haya red, la sincronización las reemplaza por las del servidor.
            templateSeeder.seedIfEmpty()

            // Al abrir la app se pide una sincronización: baja plantillas nuevas y sube lo que
            // haya quedado pendiente. Si no hay red, WorkManager espera a que vuelva.
            syncScheduler.requestSync()
        }
    }
}
