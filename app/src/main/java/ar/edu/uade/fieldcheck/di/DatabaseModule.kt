package ar.edu.uade.fieldcheck.di

import android.content.Context
import androidx.room.Room
import ar.edu.uade.fieldcheck.data.local.dao.InspectionDao
import ar.edu.uade.fieldcheck.data.local.database.FieldCheckDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Le explica a Hilt cómo crear la base de datos y los DAOs.
 *
 * Se usa `@Provides` (y no `@Inject` en el constructor) porque estas clases las crea Room,
 * no nosotros. `SingletonComponent` + `@Singleton` = una sola instancia para toda la app.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): FieldCheckDatabase {
        return Room.databaseBuilder(
            context,
            FieldCheckDatabase::class.java,
            FieldCheckDatabase.DATABASE_NAME,
        ).build()
    }

    @Provides
    fun provideInspectionDao(database: FieldCheckDatabase): InspectionDao {
        return database.inspectionDao()
    }
}
