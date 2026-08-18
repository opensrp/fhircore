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

package org.smartregister.fhircore.quest.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.smartregister.fhircore.engine.task.NamedEventInterventionService
import org.smartregister.fhircore.quest.event.EventBus

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NamedEventInterventionEntryPoint {
  fun namedEventInterventionService(): NamedEventInterventionService

  /**
   * Used by the "select available care" picker to react to questionnaire submissions (advance to
   * the next lowest-order due action, learn the current-visit Encounter id) — see
   * `feature/20260812-intervention-order-and-dedup.md` (tricc) and the companion Android spec.
   */
  fun eventBus(): EventBus
}
