package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla `evidence`: fotos tomadas como evidencia de un ítem.
 *
 * - La foto NO se guarda en la base: `filePath` apunta al archivo en `files/evidence/`.
 *   Guardar imágenes en SQLite agranda la base y vuelve lentas las consultas.
 * - `clientUuid` se genera en el teléfono para poder crear la foto sin conexión.
 * - `syncStatus` va por foto: las fotos son lo que más falla con señal débil,
 *   y así la subida puede retomar donde se cortó.
 * - Si se borra el ítem, sus fotos se borran en cascada.
 */
@Entity(
    tableName = "evidence",
    foreignKeys = [
        ForeignKey(
            entity = ItemResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemResultId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["itemResultId"])],
)
data class EvidenceEntity(
    @PrimaryKey
    val clientUuid: String,
    val itemResultId: Long,
    val filePath: String,
    val capturedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val syncStatus: String,
)
