package ar.edu.uade.fieldcheck.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Inspección finalizada tal como la recibe `POST /inspections`.
 *
 * El JSON usa los mismos nombres en camelCase que la app, así que no hace falta `@SerialName`.
 * Las fechas son milisegundos desde epoch.
 */
@Serializable
data class InspectionDto(
    val clientUuid: String,
    val templateName: String,
    val templateVersion: Int,
    val siteName: String,
    val status: String,
    val startedAt: Long,
    val finishedAt: Long,
    val updatedAt: Long,
    val items: List<ItemResultDto>,
)

/**
 * Resultado de un ítem. El servidor identifica cada ítem por su posición (empieza en 1),
 * porque el id de Room es local del teléfono.
 */
@Serializable
data class ItemResultDto(
    val position: Int,
    val itemText: String,
    val result: String,
    val note: String? = null,
)
