package ar.edu.uade.fieldcheck.domain.model

/**
 * Plantilla de checklist descargada del servidor (hoy, de la semilla local).
 *
 * Las plantillas son una caché: se pueden reemplazar enteras cuando llega una versión nueva.
 * Por eso una inspección copia los textos de los ítems al crearse (ver [ItemResult]).
 *
 * @property itemCount cantidad de ítems, para mostrar "20 ítems" en P2 sin cargar la lista entera.
 * @property downloadedAt cuándo se guardó en el teléfono; sirve para mostrar la antigüedad de la caché.
 */
data class Template(
    val id: String,
    val name: String,
    val version: Int,
    val itemCount: Int,
    val downloadedAt: Long,
)

/**
 * Un ítem de la plantilla, por ejemplo "Botiquín de primeros auxilios completo".
 *
 * @property position orden dentro de la plantilla, empezando en 0.
 */
data class TemplateItem(
    val id: String,
    val text: String,
    val position: Int,
)
