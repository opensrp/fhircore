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

package org.smartregister.fhircore.engine.sync

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.smartregister.fhircore.engine.HiltActivityForTest
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

@HiltAndroidTest
class SyncListenerManagerTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  private val syncListenerManager = SyncListenerManager()

  private val activityController = Robolectric.buildActivity(HiltActivityForTest::class.java)

  private lateinit var hiltActivityForTest: HiltActivityForTest

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
    hiltActivityForTest = activityController.get()
  }

  @After
  fun tearDown() {
    if (!hiltActivityForTest.isDestroyed) {
      activityController.destroy()
    }
  }

  @Test
  fun testRegisterSyncListenerShouldAddNewOnSyncListener() {
    activityController.create().resume()
    syncListenerManager.registerSyncListener(hiltActivityForTest, hiltActivityForTest.lifecycle)
    Assert.assertTrue(syncListenerManager.onSyncListeners.isNotEmpty())
    Assert.assertEquals(1, syncListenerManager.onSyncListeners.size)
    Assert.assertTrue(syncListenerManager.onSyncListeners.first() is HiltActivityForTest)
  }

  @Test
  fun testSyncListenerManagerShouldRemoveListenerWhenLifecycleIsDestroyed() {
    activityController.create().resume()
    syncListenerManager.registerSyncListener(hiltActivityForTest, hiltActivityForTest.lifecycle)
    activityController.destroy()
    Assert.assertTrue(syncListenerManager.onSyncListeners.isEmpty())
  }

  @Test
  fun testThatCallingDeregisterListenerRemovesOnSyncListener() {
    activityController.create().resume()
    syncListenerManager.run {
      registerSyncListener(hiltActivityForTest, hiltActivityForTest.lifecycle)
      deregisterSyncListener(hiltActivityForTest)
    }
    Assert.assertTrue(syncListenerManager.onSyncListeners.isEmpty())
  }
}
