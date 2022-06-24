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

package org.smartregister.fhircore.engine.domain.model

import org.junit.Assert
import org.junit.Test

class HealthStatusTest {

  @Test
  fun testPatientTypes() {
    Assert.assertNotNull(HealthStatus.valueOf("NEWLY_DIAGNOSED_CLIENT"))
    Assert.assertNotNull(HealthStatus.valueOf("CLIENT_ALREADY_ON_ART"))
    Assert.assertNotNull(HealthStatus.valueOf("EXPOSED_INFANT"))
    Assert.assertNotNull(HealthStatus.valueOf("COMMUNITY_POSITIVE"))
    Assert.assertNotNull(HealthStatus.valueOf("CHILD_CONTACT"))
    Assert.assertNotNull(HealthStatus.valueOf("SEXUAL_CONTACT"))
  }
}
