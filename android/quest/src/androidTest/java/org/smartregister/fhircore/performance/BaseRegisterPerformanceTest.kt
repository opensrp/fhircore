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
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import timber.log.Timber

abstract class BaseRegisterPerformanceTest {

  private lateinit var registerId: String
  private lateinit var registerRepository: RegisterRepository
  private lateinit var benchmarkRule: BenchmarkRule

  companion object {
    @JvmStatic
    @BeforeClass
    fun setup() {
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

      val databasesFolder = File("/data/data/${appContext.packageName}/databases/")

      if (!databasesFolder.exists()) {
        databasesFolder.mkdirs()
      }

      // Copy over the db
      Files.copy(
        resourcesDbInputStream,
        Path("/data/data/${appContext.packageName}/databases/resources.db"),
      )
    }

    fun String.deleteFileIsExists() {
      try {
        if (File(this).exists()) Files.delete(Path(this))
      } catch (ex: NoSuchFileException) {
        System.out.println(ex.message)
        ex.printStackTrace()
      }
    }
  }

  fun beforeTestSetup(
    registerRepository: RegisterRepository,
    benchmarkRule: BenchmarkRule,
    registerId: String,
  ) {
    this.registerId = registerId
    this.benchmarkRule = benchmarkRule
    this.registerRepository = registerRepository

    runBlocking {
      registerRepository.configurationRegistry.loadConfigurations(
        "app/debug",
        InstrumentationRegistry.getInstrumentation().targetContext,
      )
    }
  }

  fun benchmarkingFunctionality(page: Int) {
    benchmarkRule.measureRepeated {
      runBlocking {
        val repoData = registerRepository.loadRegisterData(page, registerId)
        System.out.println(
          "${repoData.size} records fetched from Page $page of Register $registerId",
        )
      }
    }
  }
}
