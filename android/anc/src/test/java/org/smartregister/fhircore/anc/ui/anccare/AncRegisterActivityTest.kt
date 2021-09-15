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

package org.smartregister.fhircore.anc.ui.anccare

import android.app.Activity
import android.view.MenuInflater
import com.google.android.fhir.sync.Sync
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.smartregister.fhircore.anc.R
import org.smartregister.fhircore.anc.activity.ActivityRobolectricTest
import org.smartregister.fhircore.anc.shadow.AncApplicationShadow
import org.smartregister.fhircore.anc.shadow.FakeKeyStore
import org.smartregister.fhircore.anc.ui.anccare.register.AncRegisterActivity

@Config(shadows = [AncApplicationShadow::class])
internal class AncRegisterActivityTest : ActivityRobolectricTest() {

  private lateinit var ancRegisterActivity: AncRegisterActivity

  private lateinit var ancRegisterActivitySpy: AncRegisterActivity

  @Before
  fun setUp() {
    mockkObject(Sync)
    every { Sync.basicSyncJob(any()).stateFlow() } returns flowOf()
    every { Sync.basicSyncJob(any()).lastSyncTimestamp() } returns OffsetDateTime.now()

    ancRegisterActivity =
      Robolectric.buildActivity(AncRegisterActivity::class.java, null).create().get()
    ancRegisterActivitySpy = spyk(objToCopy = ancRegisterActivity)
  }

  @After
  fun cleanup() {
    unmockkObject(Sync)
  }

  @Test
  fun testActivityShouldNotNull() {
    assertNotNull(ancRegisterActivity)
  }

  @Test
  fun testActivityHasCorrectSideMenuItem() {
    val sideMenu = ancRegisterActivity.sideMenuOptions()
    assertEquals(R.id.menu_item_anc, sideMenu[0].itemId)
    assertEquals(R.string.anc_register_title, sideMenu[0].titleResource)

    assertEquals(R.id.menu_item_anc, sideMenu[1].itemId)
    assertEquals(R.string.family_register_title, sideMenu[1].titleResource)
  }

  @Test
  fun testThatMenuIsCreated() {
    val menuInflater = mockk<MenuInflater>()

    every { ancRegisterActivitySpy.menuInflater } returns menuInflater
    every { menuInflater.inflate(any(), any()) } returns Unit

    Assert.assertTrue(ancRegisterActivitySpy.onCreateOptionsMenu(null))
  }

  override fun getActivity(): Activity {
    return ancRegisterActivity
  }

  companion object {
    @JvmStatic
    @BeforeClass
    fun beforeClass() {
      FakeKeyStore.setup
    }
  }
}
