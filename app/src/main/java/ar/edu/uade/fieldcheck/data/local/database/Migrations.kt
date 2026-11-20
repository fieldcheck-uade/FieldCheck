package ar.edu.uade.fieldcheck.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de la base. Room las ejecuta al abrir una base con una versión anterior.
 *
 * Versión 1 → 2: se agrega la versión de la plantilla a cada inspección (la pide la API).
 * Las inspecciones que ya existían quedan con 1, el mismo valor por defecto que declara
 * `InspectionEntity` (Room verifica que coincidan).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inspections ADD COLUMN templateVersion INTEGER NOT NULL DEFAULT 1")
    }
}
