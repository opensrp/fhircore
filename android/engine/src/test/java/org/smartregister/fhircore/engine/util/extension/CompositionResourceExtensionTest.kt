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

import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Composition.SectionComponent
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.robolectric.RobolectricTest

class CompositionResourceExtensionTest : RobolectricTest() {
  private lateinit var searchParamSection: SectionComponent
  private lateinit var defaultConfigSection: SectionComponent

  @Before
  fun setUp() {
    searchParamSection =
      SectionComponent().apply {
        code =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = "http://smartregister.org/CodeSystem/composition-section-codes"
                  code = "custom-search-parameter-bundle"
                  display = "Custom search param bundle"
                },
              )
          }
      }
    defaultConfigSection =
      SectionComponent().apply {
        code =
          CodeableConcept().apply {
            coding =
              listOf(
                Coding().apply {
                  system = "http://smartregister.org/CodeSystem/config"
                  code = "default-config-bundle"
                  display = "Default config bundle"
                },
              )
          }
      }
  }

  @Test
  fun testRetrieveCompositionSectionsExcludingCustomSearchParameters() {
    val composition =
      Composition().apply {
        section =
          listOf(
            searchParamSection,
            defaultConfigSection,
          )
      }

    val filteredCompositionSections =
      composition.retrieveCompositionSectionsExcludingCustomSearchParameters()
    Assert.assertEquals(1, filteredCompositionSections.size)
    Assert.assertEquals("default-config-bundle", filteredCompositionSections[0].code.coding[0].code)
  }

  @Test
  fun testRetrieveCustomSearchParametersSection() {
    val composition =
      Composition().apply {
        section =
          listOf(
            searchParamSection,
            defaultConfigSection,
          )
      }

    val filteredCompositionSections = composition.retrieveCustomSearchParametersSection()
    Assert.assertEquals(1, filteredCompositionSections.size)
    Assert.assertEquals(
      "custom-search-parameter-bundle",
      filteredCompositionSections[0].code.coding[0].code,
    )
  }
}
