package ar.edu.uade.fieldcheck.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ar.edu.uade.fieldcheck.data.local.dao.EvidenceDao
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.dao.TemplateDao
import ar.edu.uade.fieldcheck.data.local.entity.EvidenceEntity
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateEntity
import ar.edu.uade.fieldcheck.data.local.entity.TemplateItemEntity

/**
 * Base de datos local de la app. Es la única fuente de verdad de la UI.
 *
 * La instancia la crea Hilt en `di/DatabaseModule`, una sola vez para toda la app.
 *
 * Sigue en la versión 1 aunque se sumaron tablas: la app todavía no se publicó,
 * así que no hay bases instaladas que migrar. Al publicar, cada cambio de esquema
 * va a necesitar subir la versión y escribir una migración.
 */
@Database(
    entities = [
        TemplateEntity::class,
        TemplateItemEntity::class,
        InspectionEntity::class,
        ItemResultEntity::class,
        EvidenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FieldCheckDatabase : RoomDatabase() {

    abstract fun inspectionDao(): InspectionDao

    abstract fun templateDao(): TemplateDao

    abstract fun evidenceDao(): EvidenceDao

    companion object {
        const val DATABASE_NAME = "fieldcheck.db"
    }
}
