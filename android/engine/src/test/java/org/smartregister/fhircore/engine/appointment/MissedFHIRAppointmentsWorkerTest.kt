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

package org.smartregister.fhircore.engine.appointment

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hl7.fhir.r4.model.Appointment
import org.hl7.fhir.r4.model.Appointment.AppointmentStatus
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class MissedFHIRAppointmentsWorkerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  lateinit var fhirEngine: FhirEngine
  private lateinit var missedFHIRAppointmentsWorker: MissedFHIRAppointmentsWorker
  val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setup() {
    hiltRule.inject()
    fhirEngine = mockk()
    initializeWorkManager()
    missedFHIRAppointmentsWorker =
      TestListenableWorkerBuilder<MissedFHIRAppointmentsWorker>(context)
        .setWorkerFactory(
          object : WorkerFactory() {
            override fun createWorker(
              appContext: Context,
              workerClassName: String,
              workerParameters: WorkerParameters
            ): ListenableWorker =
              MissedFHIRAppointmentsWorker(appContext, workerParameters, fhirEngine)
          }
        )
        .build()
  }

  @Test
  fun doWork() = runTest {
    val pastDate = DateTime().minusDays(4).toDate()
    val missedAppointment =
      Appointment().apply {
        status = AppointmentStatus.BOOKED
        start = pastDate
      }
    val tomorrow = DateTime().plusDays(1).toDate()
    val bookedAppointment =
      Appointment().apply {
        status = AppointmentStatus.BOOKED
        start = tomorrow
      }
    val bookedAppointmentStartNull =
      Appointment().apply {
        status = AppointmentStatus.BOOKED
        start = null
      }
    val appointments = listOf(missedAppointment, bookedAppointment, bookedAppointmentStartNull)
    coEvery { fhirEngine.search<Appointment>(any()) } returns appointments
    coEvery { fhirEngine.update(*anyVararg()) } just runs

    val result = missedFHIRAppointmentsWorker.doWork()
    Assert.assertEquals(ListenableWorker.Result.success(), result)
    Assert.assertEquals(AppointmentStatus.NOSHOW, missedAppointment.status)
    Assert.assertEquals(AppointmentStatus.BOOKED, bookedAppointment.status)
    Assert.assertEquals(AppointmentStatus.BOOKED, bookedAppointmentStartNull.status)
  }

  private fun initializeWorkManager() {
    val config: Configuration =
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.DEBUG)
        .setExecutor(SynchronousExecutor())
        .build()

    // Initialize WorkManager for instrumentation tests.
    WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
  }
}
