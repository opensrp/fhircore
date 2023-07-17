package org.smartregister.fhircore.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import timber.log.Timber


@HiltAndroidTest
class HouseholdRegisterPerformanceTests2 {

  @get:Rule val benchmarkRule = BenchmarkRule()

  @Inject lateinit var defaultRepository: DefaultRepository
  @Inject lateinit var registerRepository: RegisterRepository

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Before
  fun setUp() {
    hiltRule.inject()

    if (InstrumentationRegistry.getInstrumentation().context.externalCacheDir == null) {
      Timber.e("Instrumentation registry is null")
    } else {
      Timber.e("Instrumentation registry is NOT NULL")
    }
    // Might need to login

    runBlocking {
      defaultRepository.configurationRegistry.loadConfigurations(
        "app/debug",
        InstrumentationRegistry.getInstrumentation().targetContext
      ) { loadConfigSuccessful ->
      }
    }

    // Copy over the db
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val testContext = InstrumentationRegistry.getInstrumentation().context

    val resourcesDbInputStream = testContext.assets.open("resources.db")

    // Delete the database files
      "/data/data/${appContext.packageName}/databases/resources.db".deleteFileIsExists()
      "/data/data/${appContext.packageName}/databases/resources.db-shm".deleteFileIsExists()
      "/data/data/${appContext.packageName}/databases/resources.db-wal".deleteFileIsExists()

    // Copy over the db
    Files.copy(
      resourcesDbInputStream,
      Path("/data/data/${appContext.packageName}/databases/resources.db")
    )
  }



  @Test
  fun benchmarkPage0() {
    benchmarkingFunctionality(0)
  }

  fun benchmarkingFunctionality(page: Int) {
    benchmarkRule.measureRepeated {

      runBlocking {
        val repoData = registerRepository.loadRegisterData(page, "householdRegister")
        System.out.println("Records fetched ${repoData.size}")
      }
    }
  }

    fun String.deleteFileIsExists() {
        try {
            if (File(this).exists()) Files.delete(Path(this))
        } catch (ex: NoSuchFileException) {}
    }
}
