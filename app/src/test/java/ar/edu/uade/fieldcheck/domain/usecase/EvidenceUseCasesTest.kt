package ar.edu.uade.fieldcheck.domain.usecase

import ar.edu.uade.fieldcheck.core.model.Resource
import ar.edu.uade.fieldcheck.fakes.AddedEvidence
import ar.edu.uade.fieldcheck.fakes.FakeInspectionRepository
import ar.edu.uade.fieldcheck.fakes.FakePhotoStorage
import ar.edu.uade.fieldcheck.fakes.createTestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvidenceUseCasesTest {

    @Test
    fun `add evidence reduces the photo and saves its path with the capture time`() = runTest {
        val repository = FakeInspectionRepository()
        val storage = FakePhotoStorage()
        val useCase = AddEvidenceUseCase(repository, storage, createTestClock(9_000L))

        val result = useCase(itemResultId = 4L, filePath = "/files/evidence/a.jpg")

        assertEquals(Resource.Success(Unit), result)
        assertEquals(listOf("/files/evidence/a.jpg"), storage.compressedFilePaths)
        assertEquals(listOf(AddedEvidence(4L, "/files/evidence/a.jpg", 9_000L)), repository.addedEvidence)
    }

    @Test
    fun `a photo that cannot be reduced is not saved`() = runTest {
        val repository = FakeInspectionRepository()
        val storage = FakePhotoStorage()
        storage.compressResult = Resource.Error("No se pudo procesar la foto. Intentá sacarla de nuevo.")
        val useCase = AddEvidenceUseCase(repository, storage, createTestClock(9_000L))

        val result = useCase(itemResultId = 4L, filePath = "/files/evidence/a.jpg")

        assertEquals(storage.compressResult, result)
        assertTrue(repository.addedEvidence.isEmpty())
    }

    @Test
    fun `add evidence without a file path is rejected`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = AddEvidenceUseCase(repository, FakePhotoStorage(), createTestClock(9_000L))

        val result = useCase(itemResultId = 4L, filePath = " ")

        assertTrue(result is Resource.Error)
        assertTrue(repository.addedEvidence.isEmpty())
    }

    @Test
    fun `prepare photo returns the file created by the storage`() = runTest {
        val storage = FakePhotoStorage()
        val useCase = PreparePhotoCaptureUseCase(storage)

        val result = useCase()

        assertEquals(storage.createResult, result)
    }

    @Test
    fun `discarding a cancelled photo deletes its file`() = runTest {
        val storage = FakePhotoStorage()
        val useCase = DiscardPhotoUseCase(storage)

        useCase("/files/evidence/cancelled.jpg")

        assertEquals(listOf("/files/evidence/cancelled.jpg"), storage.deletedFilePaths)
    }

    @Test
    fun `delete evidence asks the repository to delete it`() = runTest {
        val repository = FakeInspectionRepository()
        val useCase = DeleteEvidenceUseCase(repository)

        useCase("evidence-7")

        assertEquals(listOf("evidence-7"), repository.deletedEvidenceIds)
    }
}
