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

package org.smartregister.fhircore.engine.util.extension

import android.graphics.Bitmap
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class BitmapExtensionTest : RobolectricTest() {

  @Test
  fun testEncodeToByteArrayShouldVerifyCall() {
    val bitmap = mockk<Bitmap>(relaxed = true)
    bitmap.encodeToByteArray()
    verify { bitmap.compress(any(), any(), any()) }
  }

  @Test
  fun base64toBitmap_shouldDecodeCorrectly() {
    val base64String =
      "iVBORw0KGgoAAAANSUhEUgAAAFMAAABTCAYAAADjsjsAAAAABHNCSVQICAgIfAhkiAAAABl0RVh0U29mdHdhcmUAZ25vbWUtc2NyZWVuc2hvdO8Dvz4AAAAtdEVYdENyZWF0aW9uIFRpbWUARnJpIDE5IEFwciAyMDI0IDA3OjIxOjM4IEFNIEVBVIqENmYAAADTSURBVHic7dDBCcAgAMBAdf/p+nQZXSIglLsJQube3xkk1uuAPzEzZGbIzJCZITNDZobMDJkZMjNkZsjMkJkhM0NmhswMmRkyM2RmyMyQmSEzQ2aGzAyZGTIzZGbIzJCZITNDZobMDJkZMjNkZsjMkJkhM0NmhswMmRkyM2RmyMyQmSEzQ2aGzAyZGTIzZGbIzJCZITNDZobMDJkZMjNkZsjMkJkhM0NmhswMmRkyM2RmyMyQmSEzQ2aGzAyZGTIzZGbIzJCZITNDZobMDJkZMjN0AXiwBCviCqIRAAAAAElFTkSuQmCC"
    val bitmap = base64String.base64toBitmap()
    assertEquals(83, bitmap.width)
    assertEquals(83, bitmap.height)
  }
}
