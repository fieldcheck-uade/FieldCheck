package ar.edu.uade.fieldcheck

import android.app.Application
import ar.edu.uade.fieldcheck.data.local.database.TemplateSeeder
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
    lateinit var templateSeeder: TemplateSeeder

    /** Scope que vive lo mismo que la app; se usa para trabajo corto al iniciar. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Mientras no exista el backend, las plantillas salen de una semilla local (supuesto S5).
        // Si la pantalla de plantillas se abre antes de terminar, se actualiza sola: lee un Flow de Room.
        applicationScope.launch {
            templateSeeder.seedIfEmpty()
        }
    }
}
