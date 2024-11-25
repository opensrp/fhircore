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
  fun testPmapExecutesAgainstAllMembersOfIterable() {
    runBlocking(Dispatchers.Default) {
      val output = testIterable.pmap(Dispatchers.Default) { it + 1 }.toList()
      Assert.assertTrue(output == listOf(2, 3, 4))
    }
  }

  @Test
  fun testPmapAppearsToBeRunningInParallel() {
    val timeDelayInMillis: Long = 100
    runBlocking(Dispatchers.Default) {
      val time = measureTimeMillis {
        (1..100).pmap(Dispatchers.Default) {
          delay(timeDelayInMillis)
          it * 2
        }
      }

      // if it was sequential this would take 100x as long
      // if it is parallel it should be closer to 1x and not over 10x
      Assert.assertTrue(time < timeDelayInMillis * 10)
    }
  }

  @Test
  fun testForEachAsyncExecutesAgainstAllMembersOfIterable() {
    // Because it is async we cannot guarantee an order so a set will always pass but a list will
    // fail sometimes.
    val output = mutableSetOf<Int>()
    runBlocking(Dispatchers.Default) {
      val result = testIterable.forEachAsync(Dispatchers.Default) { output.add(it + 1) }
      Assert.assertNotEquals(output, testIterable)
      Assert.assertSame(Unit.javaClass, result.javaClass)
    }
  }

  @Test
  fun testForEachAsyncAppearsToBeRunningInParallel() {
    val timeDelayInMillis: Long = 100
    runBlocking(Dispatchers.Default) {
      val time = measureTimeMillis {
        (1..100).forEachAsync(Dispatchers.Default) {
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
