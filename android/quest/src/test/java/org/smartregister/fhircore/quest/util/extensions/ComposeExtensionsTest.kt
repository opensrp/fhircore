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

package org.smartregister.fhircore.quest.util.extensions

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.smartregister.fhircore.engine.domain.model.ToolBarHomeNavigation

class ComposeExtensionsTest {

  @Test
  fun testConditionalModifier() {
    val modifier = spyk(Modifier)
    modifier.conditional(true, { fillMaxWidth() }) { fillMaxHeight() }
    verify { modifier.fillMaxWidth() }
    modifier.conditional(false, { fillMaxWidth() }) { fillMaxHeight() }
    verify { modifier.fillMaxHeight() }
  }

  @Test
  fun testShouldEnableDrawerGesturesEnabledOnOpenDrawerScreenWhenDrawerClosed() {
    // On OPEN_DRAWER screens the edge swipe must open the drawer even while it is closed.
    assertTrue(ToolBarHomeNavigation.OPEN_DRAWER.shouldEnableDrawerGestures(isDrawerOpen = false))
  }

  @Test
  fun testShouldEnableDrawerGesturesEnabledOnOpenDrawerScreenWhenDrawerOpen() {
    assertTrue(ToolBarHomeNavigation.OPEN_DRAWER.shouldEnableDrawerGestures(isDrawerOpen = true))
  }

  @Test
  fun testShouldEnableDrawerGesturesDisabledOnNavigateBackScreenWhenDrawerClosed() {
    // On NAVIGATE_BACK screens the edge swipe must not open the drawer.
    assertFalse(
      ToolBarHomeNavigation.NAVIGATE_BACK.shouldEnableDrawerGestures(isDrawerOpen = false),
    )
  }

  @Test
  fun testShouldEnableDrawerGesturesEnabledOnNavigateBackScreenWhenDrawerOpen() {
    // An already open drawer can still be swiped closed regardless of the home navigation.
    assertTrue(ToolBarHomeNavigation.NAVIGATE_BACK.shouldEnableDrawerGestures(isDrawerOpen = true))
  }
}
