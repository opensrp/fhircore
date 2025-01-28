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

package org.smartregister.fhircore.quest.ui.questionnaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.smartregister.fhircore.quest.medintel.speech.models.GeminiModel

class SpeechToTextViewModel : ViewModel() {

  val geminiModel: GeminiModel by lazy { GeminiModel() }

  private val _speechTranscriptTextMutableStateFlow = MutableStateFlow("")
  val speechTranscriptTextStateFlow: StateFlow<String>
    get() = _speechTranscriptTextMutableStateFlow

  private val _recordingStateMutableStateFlow = MutableStateFlow(RecordingState.UNKNOWN)
  val recordingStateFlow: StateFlow<RecordingState>
    get() = _recordingStateMutableStateFlow

  init {
    viewModelScope.launch {
      delay(2800)
      appendToTranscript(speech)
    }
  }

  private val _showProcessingProgressMutableStateFlow = MutableStateFlow(false)
  val showProcessingProgressStateFlow: StateFlow<Boolean>
    get() = _showProcessingProgressMutableStateFlow

  fun showProcessingProgress() = _showProcessingProgressMutableStateFlow.update { true }

  fun hideProcessingProgress() = _showProcessingProgressMutableStateFlow.update { false }

  fun appendToTranscript(text: String) {
    _speechTranscriptTextMutableStateFlow.update { "$it $text" }
  }

  fun resetTranscript() {
    _speechTranscriptTextMutableStateFlow.update { "" }
  }

  private val speech: String =
    """
    let's start with your patient information.  What is your NHS number?

    12345

    And your full name, please?

    John Doe Smith

    What's your date of birth?

    March 15th, 1985.

    And your age?

    39.

    What is your gender?

    Male.

    What is your National ID number?

    I’m not sure that’s something I can give out.

    Is this for the patient portal?

    Yes.

    Then it’s fine.  It’s 1234567890.

    And finally, your phone number?

    0123456789

    Great, thanks.  Now, regarding your location, we need to record some details. Your home address is at 34.0522 latitude and -118.2437 longitude. Is that correct?

    Yes, that's correct.  The address is also associated with Location 678 in our records.  I also have a care location, Location 910, and my workplace is at Location 1112.

    Excellent, thank you for confirming that information.

    """
      .trimIndent()

  fun onRecordingStarted() = _recordingStateMutableStateFlow.update { RecordingState.STARTED }

  fun onRecordingResumed() = _recordingStateMutableStateFlow.update { RecordingState.RESUMED }

  fun onRecordingPaused() = _recordingStateMutableStateFlow.update { RecordingState.PAUSED }

  fun onRecordingStopped() = _recordingStateMutableStateFlow.update { RecordingState.STOPPED }
}

enum class RecordingState {
  UNKNOWN,
  STARTED,
  RESUMED,
  PAUSED,
  STOPPED,
}
