package ar.edu.uade.fieldcheck.domain.repository

/**
 * Pide que se suban las inspecciones pendientes en cuanto haya conexión.
 *
 * El dominio solo dice "hay algo para sincronizar"; la implementación en Data usa WorkManager,
 * que espera a que haya red, sobrevive a que se cierre la app y reintenta con espera exponencial.
 */
interface SyncScheduler {

    fun requestSync()
}
