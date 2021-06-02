/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.robolectric.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import org.junit.After
import org.smartregister.fhircore.robolectric.RobolectricTest

abstract class FragmentRobolectricTest : RobolectricTest() {

  @After
  fun tearDown() {
    getFragmentScenario().moveToState(Lifecycle.State.DESTROYED)
  }

  abstract fun getFragmentScenario(): FragmentScenario<out Fragment>
}
