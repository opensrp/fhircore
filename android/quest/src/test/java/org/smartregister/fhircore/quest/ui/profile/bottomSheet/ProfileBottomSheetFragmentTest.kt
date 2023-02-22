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

package org.smartregister.fhircore.quest.ui.profile.bottomSheet

import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.smartregister.fhircore.quest.ui.profile.model.EligibleManagingEntity

class ProfileBottomSheetFragmentTest {
  @Test
  fun testOnSaveClick() {

    val mockCallback = mock(Function1::class.java) as (EligibleManagingEntity) -> Unit
    val fragment = ProfileBottomSheetFragment(onSaveClick = mockCallback)

    fragment.onSaveClick(EligibleManagingEntity("group-1", "patient-1", "Jane Doe"))

    verify(mockCallback).invoke(EligibleManagingEntity("group-1", "patient-1", "Jane Doe"))
  }
}
