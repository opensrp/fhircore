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

package org.smartregister.fhircore.engine.util

import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class ParallelUtilTest {

  private val testIterable = listOf(1, 2, 3)

  @Test
  fun testExecutesAgainstAllMembersOfIterable() {
    runBlocking(Dispatchers.Default) {
      val output = testIterable.pmap { it + 1 }
      Assert.assertArrayEquals(output.toIntArray(), listOf(2, 3, 4).toIntArray())
    }
  }

  @Test
  fun testAppearsToBeRunningInParallel() {
    val timeDelayInMillis: Long = 100
    runBlocking(Dispatchers.Default) {
      val time = measureTimeMillis {
        val output =
          (1..100).pmap {
            delay(timeDelayInMillis)
            it * 2
          }
      }

      // if it was sequential this would take 100x as long
      // if it is parallel it should be closer to 1x and not over 10x
      Assert.assertTrue(time < timeDelayInMillis * 10)
    }
  }
}
