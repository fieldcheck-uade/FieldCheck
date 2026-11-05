package ar.edu.uade.fieldcheck.fakes

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.domain.model.PendingPhoto
import ar.edu.uade.fieldcheck.domain.repository.PhotoStorage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Almacenamiento de fotos falso: no toca el disco, solo anota lo que se pidió. */
class FakePhotoStorage : PhotoStorage {

    /** Resultado de [createPhotoFile]; cambiarlo simula, por ejemplo, falta de espacio. */
    var createResult: Resource<PendingPhoto> = Resource.Success(
        PendingPhoto(
            filePath = "/files/evidence/photo-1.jpg",
            contentUri = "content://ar.edu.uade.fieldcheck.fileprovider/evidence/photo-1.jpg",
        )
    )

    val deletedFilePaths = mutableListOf<String>()

    override fun createPhotoFile(): Resource<PendingPhoto> {
        return createResult
    }

    override fun deletePhotoFile(filePath: String): Boolean {
        deletedFilePaths.add(filePath)
        return true
    }
}

/** Reloj fijo en UTC para que las fechas de los tests no dependan del día en que se corren. */
fun createTestClock(nowMillis: Long): Clock {
    return Clock.fixed(Instant.ofEpochMilli(nowMillis), ZoneOffset.UTC)
}
