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

  fun setTranscriptText(text: String) {
    viewModelScope.launch {
      delay(2800)
      appendToTranscript(text)
    }
  }

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
