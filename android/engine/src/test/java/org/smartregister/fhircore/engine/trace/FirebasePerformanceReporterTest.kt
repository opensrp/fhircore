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

package org.smartregister.fhircore.engine.trace

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebasePerformanceReporterTest {

  private lateinit var firebasePerformance: FirebasePerformance
  private lateinit var performanceReporter: FirebasePerformanceReporter
  private val trace = mockk<Trace>()

  @Before
  fun setup() {
    firebasePerformance = mockk()
    performanceReporter = FirebasePerformanceReporter(firebasePerformance)

    every { trace.start() } returns Unit
    every { trace.stop() } returns Unit
    every { trace.putMetric(any(), any()) } returns Unit
    every { trace.putAttribute(any(), any()) } returns Unit
    every { trace.incrementMetric(any(), any()) } returns Unit

    every { firebasePerformance.newTrace(any()) } returns trace
  }

  @Test
  fun startTrace_shouldCreateAndStartNewTrace() {
    val traceName = "TestTrace"

    performanceReporter.startTrace(traceName)

    verify { firebasePerformance.newTrace(traceName) }
    verify { trace.start() }
  }

  @Test
  fun putMetric_shouldPutMetricOnExistingTrace() {
    val traceName = "TestTrace"
    val metricName = "TestMetric"
    val value = 10L

    performanceReporter.startTrace(traceName)
    performanceReporter.putMetric(traceName, metricName, value)

    verify { firebasePerformance.newTrace(traceName) }
    verify { trace.putMetric(metricName, value) }
  }

  @Test
  fun putAttribute_shouldPutAttributeOnExistingTrace() {
    val traceName = "TestTrace"
    val attribute = "TestAttribute"
    val value = "TestValue"

    performanceReporter.startTrace(traceName)
    performanceReporter.putAttribute(traceName, attribute, value)

    verify { firebasePerformance.newTrace(traceName) }
    verify { trace.putAttribute(attribute, value) }
  }

  @Test
  fun incrementMetric_shouldIncrementMetricOnExistingTrace() {
    val traceName = "TestTrace"
    val metric = "TestAttribute"
    val incrementBy = 1L

    performanceReporter.startTrace(traceName)
    performanceReporter.incrementMetric(traceName, metric, incrementBy)

    verify { firebasePerformance.newTrace(traceName) }
    verify { trace.incrementMetric(metric, incrementBy) }
  }

  @Test
  fun stopTrace_shouldStopExistingTrace() {
    val traceName = "TestTrace"

    performanceReporter.startTrace(traceName)
    performanceReporter.stopTrace(traceName)

    verify { firebasePerformance.newTrace(traceName) }
    verify { trace.stop() }
  }

  @Test
  fun clearTraces_shouldStopAllTracesAndClearMap() {
    val traceName1 = "Trace1"
    val traceName2 = "Trace2"

    val mockTrace1: Trace = mockk()
    val mockTrace2: Trace = mockk()

    every { firebasePerformance.newTrace(traceName1) } returns mockTrace1
    every { firebasePerformance.newTrace(traceName2) } returns mockTrace2

    every { mockTrace1.start() } returns Unit
    every { mockTrace1.stop() } returns Unit

    every { mockTrace2.start() } returns Unit
    every { mockTrace2.stop() } returns Unit

    performanceReporter.startTrace(traceName1)
    performanceReporter.startTrace(traceName2)
    performanceReporter.clearTraces()

    verify { firebasePerformance.newTrace(traceName1) }
    verify { mockTrace1.stop() }
    verify { firebasePerformance.newTrace(traceName2) }
    verify { mockTrace2.stop() }
  }

  @Test
  fun setEnabled_shouldUpdatePerformanceCollectionEnabled() {
    val enabled = true

    every { firebasePerformance.isPerformanceCollectionEnabled = any() } returns Unit

    performanceReporter.setEnabled(enabled)

    verify { firebasePerformance.isPerformanceCollectionEnabled = enabled }
  }

  @Test
  fun trace_shouldStartAndStopTraceAndReturnResult() {
    val traceName = "TestTrace"

    performanceReporter.trace(traceName) {}

    verify { trace.start() }
    verify { trace.stop() }
  }

  @Test
  fun traceSuspend_shouldStartAndStopTraceAndReturnResult() = runTest {
    val traceName = "TestTrace"

    performanceReporter.traceSuspend(traceName) {}

    verify { trace.start() }
    verify { trace.stop() }
  }
}
