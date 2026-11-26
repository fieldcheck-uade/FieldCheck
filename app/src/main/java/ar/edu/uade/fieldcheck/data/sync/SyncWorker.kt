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
            // Por ejemplo, la base no se pudo leer: se prueba de nuevo más tarde, pero no para
            // siempre. Si el error se repite, se corta acá; las inspecciones siguen pendientes en
            // la base y se vuelven a intentar cuando la app arranca, al finalizar otra inspección
            // o con el botón "Reintentar" del detalle.
            return resultForUnexpectedError()
        }
    }

    /** Decide si un error inesperado se reintenta o si ya se probó demasiadas veces. */
    private fun resultForUnexpectedError(): Result {
        // runAttemptCount arranca en 0 en la primera ejecución y suma 1 en cada reintento.
        val attemptNumber = runAttemptCount + 1
        if (attemptNumber >= MAX_ATTEMPTS_AFTER_UNEXPECTED_ERROR) {
            return Result.failure()
        }
        return Result.retry()
    }

    private companion object {
        /** Cantidad máxima de ejecuciones antes de abandonar por un error inesperado. */
        const val MAX_ATTEMPTS_AFTER_UNEXPECTED_ERROR = 5
    }
}
