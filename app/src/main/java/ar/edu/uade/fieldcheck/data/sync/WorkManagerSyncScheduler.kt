package ar.edu.uade.fieldcheck.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ar.edu.uade.fieldcheck.domain.repository.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementa [SyncScheduler] con WorkManager.
 *
 * - Condición `CONNECTED`: el trabajo espera a que haya red (RNF09: sin red no gasta batería).
 * - Espera exponencial: si hay un error transitorio, reintenta a los 30 s, 60 s, 120 s, etc.
 * - Trabajo único con `APPEND_OR_REPLACE`: si ya hay una sincronización corriendo, la nueva
 *   se encola detrás. Así una inspección finalizada mientras se sube otra no queda afuera.
 */
@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncScheduler {

    override fun requestSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    private companion object {
        const val WORK_NAME = "fieldcheck-sync"
        const val BACKOFF_SECONDS = 30L
    }
}
