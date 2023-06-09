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

class FirebasePerformanceReporter(private val firebasePerformance: FirebasePerformance) :
  PerformanceReporter {

  private val traces = mutableMapOf<String, Trace>()

  override fun startTrace(traceName: String) {
    stopTrace(traceName)
    traces[traceName] = firebasePerformance.newTrace(traceName).apply { start() }
  }

  override fun putMetric(traceName: String, metricName: String, value: Long) {
    traces[traceName]?.putMetric(metricName, value)
  }

  override fun incrementMetric(traceName: String, metricName: String, incrementBy: Long) {
    traces[traceName]?.incrementMetric(metricName, incrementBy)
  }

  override fun putAttribute(traceName: String, attribute: String, value: String) {
    traces[traceName]?.putAttribute(attribute, value)
  }

  override fun stopTrace(traceName: String) {
    traces[traceName]?.stop()
  }

  override fun clearTraces() {
    traces.values.forEach(Trace::stop)
    traces.clear()
  }

  override fun setEnabled(enabled: Boolean) {
    firebasePerformance.isPerformanceCollectionEnabled = enabled
  }

  override fun <E> trace(name: String, block: (Trace) -> E): E {
    val trace = firebasePerformance.newTrace(name)
    trace.start()
    return try {
      block(trace)
    } finally {
      trace.stop()
    }
  }

  override suspend fun <E> traceSuspend(name: String, block: suspend (Trace) -> E): E {
    val trace = firebasePerformance.newTrace(name)
    trace.start()
    return try {
      block(trace)
    } finally {
      trace.stop()
    }
  }
}
