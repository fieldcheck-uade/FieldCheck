package ar.edu.uade.fieldcheck.data.local.database

import ar.edu.uade.fieldcheck.data.local.dao.TemplateDao
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity
import java.time.Clock
import javax.inject.Inject

/**
 * Carga las plantillas en la caché local la primera vez que se abre la app.
 *
 * Simula la descarga de plantillas desde la API, que todavía no existe (supuesto S5).
 * Cuando haya backend, esta clase se reemplaza por la sincronización real y el resto
 * de la app no cambia, porque todos leen las plantillas a través de `TemplateRepository`.
 */
class TemplateSeeder @Inject constructor(
    private val templateDao: TemplateDao,
    private val clock: Clock,
) {

    /** Plantilla de la semilla: datos fijos escritos en el código. */
    private class SeedTemplate(
        val id: String,
        val name: String,
        val version: Int,
        val items: List<String>,
    )

    suspend fun seedIfEmpty() {
        val existingTemplates = templateDao.countTemplates()
        if (existingTemplates > 0) {
            return
        }

        val downloadedAt = clock.millis()
        for (seedTemplate in buildSeedTemplates()) {
            insertTemplate(seedTemplate, downloadedAt)
        }
    }

    private suspend fun insertTemplate(seedTemplate: SeedTemplate, downloadedAt: Long) {
        val template = TemplateEntity(
            id = seedTemplate.id,
            name = seedTemplate.name,
            version = seedTemplate.version,
            downloadedAt = downloadedAt,
        )

        val items = mutableListOf<TemplateItemEntity>()
        for (index in seedTemplate.items.indices) {
            val item = TemplateItemEntity(
                id = "${seedTemplate.id}-item-${index + 1}",
                templateId = seedTemplate.id,
                text = seedTemplate.items[index],
                position = index,
            )
            items.add(item)
        }

        templateDao.insertTemplateWithItems(template, items)
    }

    private fun buildSeedTemplates(): List<SeedTemplate> {
        return listOf(
            SeedTemplate(
                id = "template-control-semanal-obra",
                name = "Control semanal de obra",
                version = 3,
                items = CONSTRUCTION_ITEMS,
            ),
            SeedTemplate(
                id = "template-sala-tecnica",
                name = "Sala técnica",
                version = 2,
                items = TECHNICAL_ROOM_ITEMS,
            ),
            SeedTemplate(
                id = "template-seguridad-higiene",
                name = "Seguridad e higiene",
                version = 5,
                items = SAFETY_ITEMS,
            ),
            SeedTemplate(
                id = "template-tableros-electricos",
                name = "Tableros eléctricos",
                version = 1,
                items = ELECTRICAL_PANEL_ITEMS,
            ),
        )
    }

    private companion object {

        /** "Control semanal de obra" — 20 ítems (los primeros coinciden con el Figma de P4). */
        val CONSTRUCTION_ITEMS = listOf(
            "Acceso a obra con cartel de identificación",
            "Personal con casco y calzado de seguridad",
            "Botiquín de primeros auxilios completo",
            "Andamios con certificado de montaje",
            "Orden y limpieza en zonas de circulación",
            "Iluminación suficiente en el subsuelo",
            "Matafuegos señalizados y con carga vigente",
            "Vías de escape libres y señalizadas",
            "Barandas en bordes y aberturas",
            "Acopio de materiales ordenado",
            "Escaleras de mano en buen estado",
            "Tableros eléctricos con tapa",
            "Prolongaciones eléctricas sin empalmes expuestos",
            "Arneses y líneas de vida en trabajos en altura",
            "Excavaciones con entibado o talud seguro",
            "Señalización de riesgos visible",
            "Baños y vestuarios en condiciones",
            "Vallado perimetral completo",
            "Contenedores de residuos disponibles",
            "Libro de obra actualizado",
        )

        /** "Sala técnica" — 14 ítems. */
        val TECHNICAL_ROOM_ITEMS = listOf(
            "Acceso restringido y puerta con cerradura",
            "Matafuegos de CO2 con carga vigente",
            "Temperatura dentro del rango indicado",
            "Aire acondicionado funcionando",
            "Detectores de humo operativos",
            "Iluminación de emergencia funcionando",
            "Cableado ordenado y rotulado",
            "Racks cerrados y con puesta a tierra",
            "UPS sin alarmas activas",
            "Piso técnico sin baldosas faltantes",
            "Sin materiales combustibles almacenados",
            "Planilla de ingresos completa",
            "Señalización de riesgo eléctrico visible",
            "Sin filtraciones ni humedad",
        )

        /** "Seguridad e higiene" — 32 ítems. */
        val SAFETY_ITEMS = listOf(
            "Matafuegos señalizados y con carga vigente",
            "Hidrantes accesibles y sin obstrucciones",
            "Salidas de emergencia libres",
            "Puertas de emergencia con barral antipánico",
            "Iluminación de emergencia funcionando",
            "Plan de evacuación visible",
            "Señalización de salidas en buen estado",
            "Botiquín de primeros auxilios completo",
            "Personal con elementos de protección personal",
            "Registro de entrega de EPP actualizado",
            "Orden y limpieza en puestos de trabajo",
            "Pasillos de circulación demarcados",
            "Pisos sin derrames ni obstáculos",
            "Escaleras con pasamanos firmes",
            "Máquinas con protecciones colocadas",
            "Paradas de emergencia operativas",
            "Productos químicos rotulados",
            "Hojas de seguridad disponibles",
            "Duchas lavaojos operativas",
            "Ventilación adecuada en zonas cerradas",
            "Zonas de ruido elevado señalizadas",
            "Iluminación general suficiente",
            "Tableros eléctricos con tapa",
            "Instalación eléctrica sin cables expuestos",
            "Autoelevadores con revisión vigente",
            "Estanterías ancladas y con carga máxima indicada",
            "Residuos separados según tipo",
            "Baños y vestuarios en condiciones",
            "Agua potable disponible",
            "Capacitaciones del personal registradas",
            "Teléfonos de emergencia visibles",
            "Simulacro de evacuación anual registrado",
        )

        /** "Tableros eléctricos" — 12 ítems. */
        val ELECTRICAL_PANEL_ITEMS = listOf(
            "Tablero con tapa y cerradura",
            "Señalización de riesgo eléctrico",
            "Disyuntor diferencial funcionando (prueba con botón)",
            "Interruptores rotulados por circuito",
            "Puesta a tierra conectada",
            "Sin cables sueltos ni empalmes expuestos",
            "Sin signos de recalentamiento",
            "Espacio libre frente al tablero",
            "Tablero sin humedad ni polvo acumulado",
            "Esquema unifilar disponible",
            "Protecciones acordes a la carga",
            "Matafuego apto para fuego eléctrico cerca del tablero",
        )
    }
}
