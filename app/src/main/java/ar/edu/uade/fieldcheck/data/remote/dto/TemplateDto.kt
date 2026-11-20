package ar.edu.uade.fieldcheck.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Plantilla tal como la devuelve `GET /templates`.
 *
 * Los DTO (Data Transfer Object) son la forma del JSON de la API. Se mantienen separados de las
 * entidades de Room y de los modelos de dominio: si la API cambia, solo cambian esta clase y su mapper.
 */
@Serializable
data class TemplateDto(
    val id: String,
    val name: String,
    val version: Int,
    val items: List<TemplateItemDto>,
)

/** Ítem de una plantilla. En la API la posición empieza en 1; en la app, en 0. */
@Serializable
data class TemplateItemDto(
    val id: String,
    val text: String,
    val position: Int,
)
