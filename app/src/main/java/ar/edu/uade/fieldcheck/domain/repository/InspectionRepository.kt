package ar.edu.uade.fieldcheck.domain.repository

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de inspecciones, definido en la capa de Domain.
 *
 * Principio de inversión de dependencias (la "D" de SOLID): el dominio dice QUÉ necesita
 * y la capa de Data decide CÓMO conseguirlo (hoy Room, mañana también la API).
 * Gracias a esto los casos de uso se testean con un repositorio falso, sin emulador.
 */
interface InspectionRepository {

    /**
     * Observa todas las inspecciones guardadas en el teléfono.
     *
     * Devuelve un [Flow]: cada vez que cambia la base local se emite la lista nueva,
     * así la pantalla se actualiza sola.
     */
    fun observeInspections(): Flow<Resource<List<Inspection>>>
}
