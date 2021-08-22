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

package org.smartregister.fhircore.shadow

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import java.text.SimpleDateFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire

object TestUtils {
  private val iParser: IParser = FhirContext.forR4().newJsonParser()

  fun loadQuestionnaire(context: Context, questionnaire: String): Questionnaire {
    val qJson = context.assets.open(questionnaire).bufferedReader().use { it.readText() }
    return iParser.parseResource(qJson) as Questionnaire
  }

  fun <T> LiveData<T>.getOrAwaitValue(time: Long = 2, timeUnit: TimeUnit = TimeUnit.SECONDS): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer =
      object : Observer<T> {
        override fun onChanged(o: T?) {
          data = o
          latch.countDown()
          this@getOrAwaitValue.removeObserver(this)
        }
      }

    this.observeForever(observer)

    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
      throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST") return data as T
  }

  val TEST_PATIENT_1 =
    Patient().apply {
      id = "test_patient_1_id"
      gender = Enumerations.AdministrativeGender.MALE
      name =
        mutableListOf(
          HumanName().apply {
            addGiven("jane")
            setFamily("Mc")
          }
        )
      telecom = mutableListOf(ContactPoint().apply { value = "12345678" })
      address =
        mutableListOf(
          Address().apply {
            city = "Nairobi"
            country = "Kenya"
          }
        )
      active = true
      birthDate = SimpleDateFormat("yyyy-MM-dd").parse("2021-05-25")
    }
}
