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

package org.smartregister.fhircore.eir.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.EirApplication
import org.smartregister.fhircore.eir.RobolectricTest
import org.smartregister.fhircore.eir.shadow.FhirApplicationShadow
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper

@Config(shadows = [FhirApplicationShadow::class])
class SharedPreferencesHelperTest : RobolectricTest() {

  @Before
  fun setUp() {
    init()
  }

  private fun init() {
    SharedPreferencesHelper.init(EirApplication.getContext())
    runBlocking { EirApplication.fhirEngine(EirApplication.getContext()) }
  }

  @Test fun initShouldCreatePrefs() {}

  @Test
  fun `read returns written string`() {
    val writtenString = "a"
    SharedPreferencesHelper.write("a", writtenString)
    Assert.assertEquals(writtenString, SharedPreferencesHelper.read("a", "b"))
  }

  @Test
  fun `read returns written long`() {
    val writtenLong: Long = 1
    SharedPreferencesHelper.write("a", writtenLong)
    Assert.assertEquals(writtenLong, SharedPreferencesHelper.read("a", 2))
  }
}
