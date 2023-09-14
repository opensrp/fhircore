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

package org.smartregister.fhircore.quest.util.extensions

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.navigation.NavController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ActionConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.SnackBarMessageConfig
import org.smartregister.fhircore.quest.robolectric.RobolectricTest

class LifeCycleExtensionTest : RobolectricTest() {
  private val navController = mockk<NavController>(relaxUnitFun = true)
  private val context = mockk<Context>(relaxUnitFun = true, relaxed = true)

  @Before
  fun setUp() {
    every { navController.context } returns context
  }

  @Test
  fun testHookSnackBarExecutesHandleClickEvent() = runTest {
    val messageConfig = mockk<SnackBarMessageConfig>()
    every { messageConfig.message } returns "This is a message"

    val mockedSharedFlow = mockk<SharedFlow<SnackBarMessageConfig>>()

    val scaffoldState = mockk<ScaffoldState>()
    val resourceData = mockk<ResourceData>()
    val listActionConfig = mockk<List<ActionConfig>>()
    every { scaffoldState.snackbarHostState } returns mockk()
    coEvery {
      scaffoldState.snackbarHostState.showSnackbar("message", "action", SnackbarDuration.Long)
    } returns mockk()
    val snackBarResult = runBlocking {
      scaffoldState.snackbarHostState.showSnackbar("message", "action", SnackbarDuration.Long)
    }

    assertNotNull(snackBarResult)
    every { listActionConfig.handleClickEvent(navController, resourceData) } just runs
    coEvery {
      mockedSharedFlow.hookSnackBar(scaffoldState, resourceData = resourceData, navController)
    } just runs
    coVerify { scaffoldState.snackbarHostState.showSnackbar(any(), any(), any()) }
  }
}
