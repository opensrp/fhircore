/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.anc.robolectric

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.clearAllMocks
import java.io.File
import java.io.FileReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.anc.app.fakes.FakeKeyStore
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

@RunWith(FhircoreTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = HiltTestApplication::class)
abstract class RobolectricTest {
  /** Get the liveData value by observing but wait for 3 seconds if not ready then stop observing */
  @Throws(InterruptedException::class)
  fun <T> getLiveDataValue(liveData: LiveData<T>): T? {
    val data = arrayOfNulls<Any>(1)
    val latch = CountDownLatch(1)
    val observer: Observer<T> =
      object : Observer<T> {
        override fun onChanged(o: T?) {
          data[0] = o
          latch.countDown()
          liveData.removeObserver(this)
        }
      }
    liveData.observeForever(observer)
    latch.await(3, TimeUnit.SECONDS)
    return data[0] as T?
  }

  fun String.readFile(): String {
    val file = File("$ASSET_BASE_PATH/$this")
    val charArray = CharArray(file.length().toInt()).apply { FileReader(file).read(this) }
    return String(charArray)
  }

  fun buildStructureMapUtils(): StructureMapUtilities {
    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val transformSupportServices = TransformSupportServices(contextR4)

    return StructureMapUtilities(contextR4, transformSupportServices)
  }

  fun StructureMapUtilities.worker(): IWorkerContext =
    ReflectionHelpers.getField<IWorkerContext>(this, "worker")

  companion object {
    val ASSET_BASE_PATH =
      (System.getProperty("user.dir") +
        File.separator +
        "src" +
        File.separator +
        "test" +
        File.separator +
        "resources" +
        File.separator)

    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }

    @JvmStatic
    @AfterClass
    fun resetMocks() {
      clearAllMocks()
    }
  }
}
