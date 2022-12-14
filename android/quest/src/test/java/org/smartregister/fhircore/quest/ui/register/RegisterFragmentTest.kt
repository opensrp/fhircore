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

package org.smartregister.fhircore.quest.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import com.google.android.fhir.sync.Result
import com.google.android.fhir.sync.State
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

@HiltAndroidTest
class RegisterFragmentTest : RobolectricTest() {

  val inflater = mockk<LayoutInflater>()
  val container: ViewGroup? = mockk()
  val view = mockk<View>()
  val savedInstance: Bundle? = mockk()
  val registerViewModel = RegisterViewModel(mockk(), mockk(), mockk(), mockk())

  @OptIn(ExperimentalMaterialApi::class) lateinit var registerFragment: RegisterFragment

  @OptIn(ExperimentalMaterialApi::class)
  @Before
  fun setUp() {
    registerFragment = mockk()
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testOnStopClearsSearchText() {
    coEvery { registerFragment.onStop() } just runs
    registerFragment.onStop()
    verify { registerFragment.onStop() }
    Assert.assertEquals(registerViewModel.searchText.value, "")
  }

  @OptIn(ExperimentalMaterialApi::class)
  @Test
  fun testOnSyncState() {
    val state = State.Finished(result = Result.Success())
    coEvery { registerFragment.onSync(state) } just runs
    registerFragment.onSync(state = state)
    verify { registerFragment.onSync(state) }
  }
}
