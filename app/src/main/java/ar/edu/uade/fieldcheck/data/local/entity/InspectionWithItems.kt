package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Una inspección con todos sus ítems y, dentro de cada ítem, sus fotos.
 *
 * No es una tabla. `@Relation` le pide a Room que, además de la fila de la inspección,
 * traiga las filas relacionadas de `item_results` y `evidence`. Room no garantiza el orden
 * de las listas relacionadas, por eso el mapper ordena los ítems por posición.
 */
data class InspectionWithItems(
    @Embedded
    val inspection: InspectionEntity,
    @Relation(
        entity = ItemResultEntity::class,
        parentColumn = "clientUuid",
        entityColumn = "inspectionId",
    )
    val items: List<ItemResultWithEvidence>,
)

/** Un ítem con sus fotos de evidencia. */
data class ItemResultWithEvidence(
    @Embedded
    val itemResult: ItemResultEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "itemResultId",
    )
    val evidence: List<EvidenceEntity>,
)
