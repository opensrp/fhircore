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

package org.smartregister.fhircore.quest.ui.questionnaire

/** This sealed class represents the different progress states while working with Questionnaires. */
sealed class QuestionnaireProgressState(val active: Boolean = false) {
  /** Determines whether to display the progress dialog when questionnaire is launched */
  class QuestionnaireLaunch(active: Boolean) : QuestionnaireProgressState(active)

  /**
   * This [QuestionnaireProgressState] determine when to display/dismiss progress dialog during and
   * after extraction
   */
  class ExtractionInProgress(active: Boolean) : QuestionnaireProgressState(active)
}
