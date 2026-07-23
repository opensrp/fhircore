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

package org.smartregister.fhircore.engine.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenSrpDownloadManagerTest {

  @Test
  fun `disablePrettyPrint rewrites _pretty=true to false`() {
    assertEquals(
      "Patient?_count=200&_pretty=false",
      "Patient?_count=200&_pretty=true".disablePrettyPrint(),
    )
  }

  @Test
  fun `disablePrettyPrint leaves _pretty=false unchanged`() {
    assertEquals(
      "Patient?_pretty=false",
      "Patient?_pretty=false".disablePrettyPrint(),
    )
  }

  @Test
  fun `disablePrettyPrint appends param when absent with existing query`() {
    assertEquals(
      "Patient?_count=200&_pretty=false",
      "Patient?_count=200".disablePrettyPrint(),
    )
  }

  @Test
  fun `disablePrettyPrint appends param when no query string`() {
    assertEquals("Patient?_pretty=false", "Patient".disablePrettyPrint())
  }

  @Test
  fun `disablePrettyPrint rewrites mid-url _pretty preserving following params`() {
    assertEquals(
      "Patient?_pretty=false&_count=200",
      "Patient?_pretty=true&_count=200".disablePrettyPrint(),
    )
  }
}
