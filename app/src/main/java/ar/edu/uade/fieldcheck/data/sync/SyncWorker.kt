package ar.edu.uade.fieldcheck.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.edu.uade.fieldcheck.domain.usecase.RefreshTemplatesUseCase
import ar.edu.uade.fieldcheck.domain.usecase.SyncPendingInspectionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Trabajo en segundo plano que sincroniza con el servidor (RF03).
 *
 * Lo programa [WorkManagerSyncScheduler] con la condición "hay red": WorkManager lo arranca
 * solo cuando vuelve la conexión, aunque la app esté cerrada, y lo vuelve a correr con espera
 * exponencial cuando devuelve [Result.retry].
 *
 * No tiene reglas de negocio: solo llama a los casos de uso y traduce su respuesta a WorkManager.
 * `@HiltWorker` + `@AssistedInject` permiten que Hilt le inyecte los casos de uso; el contexto y
 * los parámetros los pone WorkManager (por eso son `@Assisted`).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshTemplates: RefreshTemplatesUseCase,
    private val syncPendingInspections: SyncPendingInspectionsUseCase,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // Primero se actualizan las plantillas. Si falla no importa: queda la caché anterior.
            refreshTemplates()

            val outcome = syncPendingInspections()
            return when (outcome) {
                SyncPendingInspectionsUseCase.Outcome.DONE -> Result.success()
                SyncPendingInspectionsUseCase.Outcome.RETRY_LATER -> Result.retry()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            // Por ejemplo, la base no se pudo leer: se prueba de nuevo más tarde.
            return Result.retry()
        }
    }
}
