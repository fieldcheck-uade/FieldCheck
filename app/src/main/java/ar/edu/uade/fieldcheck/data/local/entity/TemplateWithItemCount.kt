package ar.edu.uade.fieldcheck.data.local.entity

import androidx.room.Embedded

/**
 * Resultado de la consulta de plantillas: la plantilla más la cantidad de ítems ("20 ítems").
 *
 * No es una tabla. Room arma este objeto a partir de las columnas que devuelve
 * `TemplateDao.observeTemplatesWithItemCount()`.
 */
data class TemplateWithItemCount(
    @Embedded
    val template: TemplateEntity,
    val itemCount: Int,
)
