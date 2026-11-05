package ar.edu.uade.fieldcheck.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.entity.InspectionEntity
import ar.edu.uade.fieldcheck.data.local.entity.ItemResultEntity

/**
 * Base de datos local de la app. Es la única fuente de verdad de la UI.
 *
 * La instancia la crea Hilt en `di/DatabaseModule`, una sola vez para toda la app.
 */
@Database(
    entities = [
        InspectionEntity::class,
        ItemResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FieldCheckDatabase : RoomDatabase() {

    abstract fun inspectionDao(): InspectionDao

    companion object {
        const val DATABASE_NAME = "fieldcheck.db"
    }
}
