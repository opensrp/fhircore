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

package org.smartregister.fhircore.engine.util.extension

import org.hl7.fhir.r4.model.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskExtensionTest {

  @Test
  fun testIsActiveAnc_shouldReturnTrue() {
    var task =
      Task().apply {
        status = Task.TaskStatus.READY
        description = "ANC Follow UP"
      }

    assertTrue(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.REQUESTED
        description = "Follow UP ANC"
      }

    assertTrue(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.READY
        description = "Pregnancy Follow UP"
      }

    assertTrue(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.READY
        description = "Follow UP Pregnant Woman"
      }

    assertTrue(task.isActiveAnc())
  }

  @Test
  fun testIsActiveAnc_shouldReturnFalse() {
    var task =
      Task().apply {
        status = Task.TaskStatus.READY
        description = "Child Follow UP"
      }

    assertFalse(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.CANCELLED
        description = "Follow UP ANC"
      }

    assertFalse(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.FAILED
        description = "Pregnancy Follow UP"
      }

    assertFalse(task.isActiveAnc())

    task =
      Task().apply {
        status = Task.TaskStatus.COMPLETED
        description = "Follow UP Pregnant Woman"
      }

    assertFalse(task.isActiveAnc())
  }
}
