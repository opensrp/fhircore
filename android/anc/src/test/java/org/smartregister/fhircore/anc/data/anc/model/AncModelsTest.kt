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

package org.smartregister.fhircore.anc.data.anc.model

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.anc.robolectric.RobolectricTest

class AncModelsTest : RobolectricTest() {

  private lateinit var ancOverviewItem: AncOverviewItem

  @Before
  fun setUp() {
    ancOverviewItem = AncOverviewItem("2020-02-12", "2", "1", "1")
  }

  @Test
  fun testAncOverviewItem() {
    Assert.assertEquals("1", ancOverviewItem.noOfFetuses)
    Assert.assertEquals("1", ancOverviewItem.risk)
    Assert.assertEquals("2", ancOverviewItem.ga)
    Assert.assertEquals("2020-02-12", ancOverviewItem.edd)
  }
}
