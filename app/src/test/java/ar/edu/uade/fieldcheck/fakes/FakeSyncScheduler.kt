package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.domain.repository.SyncScheduler

/** Programador falso: solo cuenta cuántas veces se pidió sincronizar. */
class FakeSyncScheduler : SyncScheduler {

    var requestCount = 0
        private set

    override fun requestSync() {
        requestCount++
    }
}
