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

package org.smartregister.fhircore.engine.domain.model

import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig

class QuestionnaireTypeTest {

  @Test
  fun testDefault() {
    Assert.assertTrue(QuestionnaireConfig("id", type = "DEFAULT").isDefault())
    Assert.assertFalse(QuestionnaireConfig("id", type = "EDIT").isDefault())
    Assert.assertFalse(QuestionnaireConfig("id", type = "READ_ONLY").isDefault())
  }

  @Test
  fun testEdit() {
    Assert.assertFalse(QuestionnaireConfig("id", type = "DEFAULT").isEditable())
    Assert.assertTrue(QuestionnaireConfig("id", type = "EDIT").isEditable())
    Assert.assertFalse(QuestionnaireConfig("id", type = "READ_ONLY").isEditable())
  }

  @Test
  fun testReadOnly() {
    Assert.assertFalse(QuestionnaireConfig("id", type = "DEFAULT").isReadOnly())
    Assert.assertFalse(QuestionnaireConfig("id", type = "EDIT").isReadOnly())
    Assert.assertTrue(QuestionnaireConfig("id", type = "READ_ONLY").isReadOnly())
  }
}
