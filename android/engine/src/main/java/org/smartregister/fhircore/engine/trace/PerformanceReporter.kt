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

interface PerformanceReporter {

  fun startTrace(traceName: String)

  fun putMetric(traceName: String, metricName: String, value: Long)

  fun incrementMetric(traceName: String, metricName: String, incrementBy: Long)

  fun putAttribute(traceName: String, attribute: String, value: String)

  fun stopTrace(traceName: String)

  fun clearTraces()

  fun setEnabled(enabled: Boolean)

  fun <E> trace(name: String, block: (Trace) -> E): E

  suspend fun <E> traceSuspend(name: String, block: suspend (Trace) -> E): E
}
