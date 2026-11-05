package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.ItemResultValue
import ar.edu.uade.fieldcheck.domain.repository.InspectionRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: guardar el resultado y la nota de un ítem (regla R3).
 *
 * Reglas:
 * - Se guarda en cada cambio: no existe un botón "Guardar".
 * - Una nota en blanco se guarda como `null`. Así "tiene nota" significa siempre
 *   que hay texto real, y la validación de evidencia (R4) no se engaña con espacios.
 * - `result = null` está permitido: tocar el resultado ya elegido lo deja sin completar.
 */
class SaveItemResultUseCase @Inject constructor(
    private val repository: InspectionRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(
        itemResultId: Long,
        result: ItemResultValue?,
        note: String?,
    ): Resource<Unit> {
        val normalizedNote = normalizeNote(note)
        val now = clock.millis()

        return repository.saveItemResult(
            itemResultId = itemResultId,
            result = result,
            note = normalizedNote,
            now = now,
        )
    }

    private fun normalizeNote(note: String?): String? {
        if (note == null) {
            return null
        }
        if (note.isBlank()) {
            return null
        }
        return note
    }
}
