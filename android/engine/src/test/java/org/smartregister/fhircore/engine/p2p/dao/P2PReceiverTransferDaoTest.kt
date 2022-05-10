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

package org.smartregister.fhircore.engine.p2p.dao

import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider

class P2PReceiverTransferDaoTest : RobolectricTest() {
  private val dispatcherProvider = spyk(DefaultDispatcherProvider())
  private val fhirContext = spyk(FhirContext())
  private val fhirEngine: FhirEngine = mockk()
  private lateinit var p2PReceiverTransferDao: P2PReceiverTransferDao

  @Before
  fun setUp() {
    p2PReceiverTransferDao =
      P2PReceiverTransferDao(
        fhirEngine = fhirEngine,
        dispatcherProvider = dispatcherProvider,
        fhirContext = fhirContext
      )
  }

  @Test
  fun `getP2PDataTypes() should return defined data types`() {
    val dataTypes = p2PReceiverTransferDao.getDataTypes()
    Assert.assertNotNull(dataTypes)
  }
}
