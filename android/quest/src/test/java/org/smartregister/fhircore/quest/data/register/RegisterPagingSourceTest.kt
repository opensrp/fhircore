package org.smartregister.fhircore.quest.data.register

import androidx.paging.PagingSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.app.fakes.Faker
import org.smartregister.fhircore.quest.data.register.model.RegisterPagingSourceState

class RegisterPagingSourceTest {

  private val registerRepository = mockk<RegisterRepository>()

  private lateinit var registerPagingSource: RegisterPagingSource

  private val registerId = "registerId"

  @Before
  fun setUp() {
    registerPagingSource = RegisterPagingSource(registerRepository)
  }

  @Test
  fun testLoadShouldReturnResults() {
    coEvery { registerRepository.loadRegisterData(0, registerId) } returns
      listOf(ResourceData(Faker.buildPatient(), emptyMap(), emptyMap()))

    val loadParams = mockk<PagingSource.LoadParams<Int>>()
    every { loadParams.key } returns null
    runBlocking {
      registerPagingSource.run {
        setPatientPagingSourceState(
          RegisterPagingSourceState(registerId = registerId, currentPage = 0, loadAll = false)
        )
        val result = load(loadParams)
        Assert.assertNotNull(result)
        Assert.assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
      }
    }
  }
}
