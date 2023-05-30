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

import com.google.firebase.perf.metrics.Trace
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs

class FakePerformanceReporter : PerformanceReporter {
  override fun startTrace(traceName: String) {}

  override fun putMetric(traceName: String, metricName: String, value: Long) {}

  override fun incrementMetric(traceName: String, metricName: String, incrementBy: Long) {}

  override fun putAttribute(traceName: String, attribute: String, value: String) {}

  override fun stopTrace(traceName: String) {}

  override fun clearTraces() {}

  override fun setEnabled(enabled: Boolean) {}

  override fun <E> trace(name: String, block: (Trace) -> E): E {
    val trace = mockk<Trace>() { every { putAttribute(any(), any()) } just runs }
    return try {
      block(trace)
    } finally {}
  }

  override suspend fun <E> traceSuspend(name: String, block: suspend (Trace) -> E): E {
    val trace = mockk<Trace>() { every { putAttribute(any(), any()) } just runs }
    return try {
      block(trace)
    } finally {}
  }
}
