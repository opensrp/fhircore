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

import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Reference

private const val SEARCH_PARAMETER_SECTION_SYSTEM_URL =
  "http://smartregister.org/CodeSystem/composition-section-codes"
private const val SEARCH_PARAMETER_SECTION_CODE = "custom-search-parameter-bundle"

/**
 * Composition sections can be nested. This function retrieves all the nested composition sections
 * and returns a flattened list of all [Composition.SectionComponent] for the given [Composition]
 * resource
 */
fun Composition.retrieveCompositionSections(): List<Composition.SectionComponent> {
  val sections = mutableListOf<Composition.SectionComponent>()
  val sectionsQueue = ArrayDeque<Composition.SectionComponent>()
  this.section.forEach {
    if (!it.section.isNullOrEmpty()) {
      it.section.forEach { sectionComponent -> sectionsQueue.addLast(sectionComponent) }
    }
    sections.add(it)
  }
  while (sectionsQueue.isNotEmpty()) {
    val sectionComponent = sectionsQueue.removeFirst()
    if (!sectionComponent.section.isNullOrEmpty()) {
      sectionComponent.section.forEach { sectionsQueue.addLast(it) }
    }
    sections.add(sectionComponent)
  }
  return sections
}

fun Composition.retrieveCompositionSectionsExcludingCustomSearchParameters():
  List<Composition.SectionComponent> {
  return retrieveCompositionSections()
    .filterNot(Composition.SectionComponent::isASearchParameterSection)
}

fun Composition.retrieveCustomSearchParametersSection(): List<Composition.SectionComponent> {
  return retrieveCompositionSections()
    .filter(Composition.SectionComponent::isASearchParameterSection)
}

fun Composition.SectionComponent.sectionDataReference(): Iterable<Reference> {
  return if (hasFocus() && focus.hasReferenceElement()) entry + focus else entry
}

fun Composition.SectionComponent.isASearchParameterSection(): Boolean {
  return code.coding.any {
    it.system.lowercase() == SEARCH_PARAMETER_SECTION_SYSTEM_URL.lowercase() &&
      it.code.lowercase() == SEARCH_PARAMETER_SECTION_CODE.lowercase()
  }
}
