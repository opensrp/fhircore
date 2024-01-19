/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.geowidget.rule

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.smartregister.fhircore.engine.util.DispatcherProvider

@ExperimentalCoroutinesApi
class CoroutineTestRule(val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) :
  TestRule, TestCoroutineScope by TestCoroutineScope(testDispatcher) {

  val testDispatcherProvider =
    object : DispatcherProvider {
      override fun default() = testDispatcher

      override fun io() = testDispatcher

      override fun main() = testDispatcher

      override fun unconfined() = testDispatcher
    }

  override fun apply(base: Statement?, description: Description?) =
    object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        Dispatchers.setMain(testDispatcher)
        base?.evaluate()
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
      }
    }
}
