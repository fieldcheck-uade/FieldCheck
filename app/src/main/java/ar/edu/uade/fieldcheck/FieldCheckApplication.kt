package ar.edu.uade.fieldcheck

import android.app.Application
import ar.edu.uade.fieldcheck.data.local.database.SampleDataSeeder
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
 * dependencias en Activities y ViewModels.
 */
@HiltAndroidApp
class FieldCheckApplication : Application() {

    @Inject
    lateinit var sampleDataSeeder: SampleDataSeeder

    /** Scope que vive lo mismo que la app; se usa para trabajo corto al iniciar. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // TEMPORAL: datos de ejemplo hasta que exista la pantalla "Nueva inspección" (P2).
        applicationScope.launch {
            sampleDataSeeder.seedIfEmpty()
        }
    }
}
