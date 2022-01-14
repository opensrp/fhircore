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

package org.smartregister.fhircore.engine.util.extension

import android.graphics.Bitmap
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class BitmapExtensionTest : RobolectricTest() {

  @Test
  fun testEncodeToByteArrayShouldVerifyCall() {
    val bitmap = mockk<Bitmap>(relaxed = true)
    bitmap.encodeToByteArray()
    verify { bitmap.compress(any(), any(), any()) }
  }
}
