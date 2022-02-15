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

package org.smartregister.fhircore.quest.robolectric

import android.os.Build
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.clearAllMocks
import java.io.File
import java.io.FileReader
import java.util.Base64
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.context.IWorkerContext
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.utils.StructureMapUtilities
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.fhircore.engine.util.extension.asYyyyMmDd
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices
import org.smartregister.fhircore.quest.app.fakes.FakeKeyStore
import org.smartregister.fhircore.quest.coroutine.CoroutineTestRule

@RunWith(FhircoreTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = HiltTestApplication::class)
abstract class RobolectricTest {

  @get:Rule(order = 10) val coroutineTestRule = CoroutineTestRule()

  @get:Rule(order = 20) val instantTaskExecutorRule = InstantTaskExecutorRule()

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

  fun String.readFileToBase64Encoded(): String {
    return Base64.getEncoder().encodeToString(this.readFile().toByteArray())
  }

  fun String.readStringToBase64Encoded(): String {
    return Base64.getEncoder().encodeToString(this.toByteArray())
  }

  fun String.parseSampleResource(): IBaseResource =
    this.readFile()
      .let {
        it.replace("#TODAY", Date().asYyyyMmDd()).replace("#NOW", DateTimeType.now().valueAsString)
      }
      .let { FhirContext.forR4Cached().newJsonParser().parseResource(it) }

  fun IBaseResource.convertToString(trimTime: Boolean) =
    FhirContext.forR4Cached().newJsonParser().encodeResourceToString(this).let {
      // replace time part 11:11:11+05:00 with xx:xx:xx+xx:xx
      if (trimTime) it.replace(Regex("\\d{2}:\\d{2}:\\d{2}.\\d{2}:\\d{2}"), "xx:xx:xx+xx:xx")
      else it
    }

  fun String.replaceTimePart() =
    // replace time part 11:11:11+05:00 with xx:xx:xx+xx:xx
    // replace time part 11:11:11.111+05:00 with xx:xx:xx+xx:xx
    this.replace(Regex("\\d{2}:\\d{2}:\\d{2}.\\d{2}:\\d{2}"), "xx:xx:xx+xx:xx")
      .replace(Regex("\\d{2}:\\d{2}:\\d{2}.\\d{3}.\\d{2}:\\d{2}"), "xx:xx:xx+xx:xx")

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

  fun StructureMapUtilities.worker(): IWorkerContext = ReflectionHelpers.getField(this, "worker")

  fun transform(
    scu: StructureMapUtilities,
    structureMapJson: String,
    responseJson: String,
    sourceGroup: String
  ): Bundle {
    val map = scu.parse(structureMapJson, sourceGroup)

    val iParser: IParser = FhirContext.forR4Cached().newJsonParser()

    println(iParser.encodeResourceToString(map))

    val targetResource = Bundle()

    val source = iParser.parseResource(QuestionnaireResponse::class.java, responseJson)

    kotlin.runCatching { scu.transform(scu.worker(), source, map, targetResource) }.onFailure {
      println(it.stackTraceToString())
    }

    println(iParser.encodeResourceToString(targetResource))

    return targetResource
  }

  @After
  open fun tearDown() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

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
