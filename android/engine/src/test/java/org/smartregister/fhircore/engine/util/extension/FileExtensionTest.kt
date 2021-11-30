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

import io.mockk.mockkStatic
import io.mockk.verify
import java.io.File
import java.io.FileOutputStream
import org.junit.Test

class FileExtensionTest {

  @Test
  fun testFileShouldReturnBase64Encoded() {
    mockkStatic(File::encodeToBase64)

    val file = File.createTempFile("sample_file", ".txt")
    val writer = FileOutputStream(file)
    writer.write("file".toByteArray())

    file.encodeToBase64()

    verify { file.encodeToBase64() }

    file.delete()
  }
}
