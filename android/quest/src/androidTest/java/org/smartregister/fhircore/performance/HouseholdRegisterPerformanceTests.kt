/*
 * Copyright 2021-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.search.Search
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.ResourceType
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runners.Parameterized
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import timber.log.Timber

@HiltAndroidTest
class HouseholdRegisterPerformanceTests {

  @get:Rule(order = 1) val benchmarkRule = BenchmarkRule()

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  //@Inject lateinit var defaultRepository: DefaultRepository

  @Inject lateinit var registerRepository: RegisterRepository

  //var injected = false

  @Before
  fun setUp() {
    hiltRule.inject()
    // Might need to login

    runBlocking {
      registerRepository.configurationRegistry.loadConfigurations(
        "app/debug",
        InstrumentationRegistry.getInstrumentation().targetContext,
      ) { loadConfigSuccessful ->
      }
    }
  }

  @Test
  fun benchmarkPage0() {

    runBlocking {
      val count = registerRepository.fhirEngine.count(Search(ResourceType.Patient))
      System.out.println("Patient count: $count")
    }
    benchmarkingFunctionality(1)
  }

  @Test
  fun benchmarkPage1() {
    benchmarkingFunctionality(0)
  }

  @Test
  fun benchmarkPage2() {
    benchmarkingFunctionality(1)
  }

  @Test
  fun benchmarkPage3() {
    benchmarkingFunctionality(2)
  }

  @Test
  fun benchmarkPage4() {
    benchmarkingFunctionality(3)
  }

  @Test
  fun benchmarkPage5() {
    benchmarkingFunctionality(4)
  }

  @Test
  fun benchmarkPage6() {
    benchmarkingFunctionality(5)
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

  companion object {
    var injected = false

    @JvmStatic
    @BeforeClass
    fun setup(): Unit {
      if (InstrumentationRegistry.getInstrumentation().context.externalCacheDir == null) {
        Timber.e("Instrumentation registry is null")
      } else {
        Timber.e("Instrumentation registry is NOT NULL")
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
        Path("/data/data/${appContext.packageName}/databases/resources.db"),
      )
    }
  }
}
