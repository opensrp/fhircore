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

import android.content.Context
import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.quest.medintel.speech.models.GeminiModel
import org.smartregister.fhircore.quest.medintel.speech.speechtoform.LiveSpeechToText
import org.smartregister.fhircore.quest.medintel.speech.speechtoform.TextToForm
import timber.log.Timber

@HiltViewModel
class SpeechToTextViewModel
@Inject
constructor(val liveSpeechToText: LiveSpeechToText, val textToForm: TextToForm) : ViewModel() {

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

  fun onRecordingStarted() = _recordingStateMutableStateFlow.update { RecordingState.STARTED }

  fun onRecordingResumed() = _recordingStateMutableStateFlow.update { RecordingState.RESUMED }

  fun onRecordingPaused() = _recordingStateMutableStateFlow.update { RecordingState.PAUSED }

  fun onRecordingStopped() = _recordingStateMutableStateFlow.update { RecordingState.STOPPED }

  fun startRecording(audioRecord: AudioRecord): Job {
    return viewModelScope.launch {
      liveSpeechToText
        .startTranscription(audioRecord)
        .onStart {
          resetTranscript()
          onRecordingStarted()
        }
        .collect { appendToTranscript(it) }
    }
  }

  fun resumeRecording(audioRecord: AudioRecord): Job {
    return viewModelScope.launch {
      liveSpeechToText
        .startTranscription(audioRecord)
        .onStart { onRecordingResumed() }
        .collect { appendToTranscript(it) }
    }
  }

  fun processTranscriptQuestionnaireResponse(
    context: Context,
    questionnaire: Questionnaire,
    transcript: String,
    onSuccess: (QuestionnaireResponse) -> Unit,
    onError: (Throwable) -> Unit,
  ) {
    viewModelScope.launch {
      showProcessingProgress()
      try {
        val result =
          textToForm.generateQuestionnaireResponse(
            transcript,
            questionnaire,
            context,
            geminiModel,
          )
        onSuccess(result)
      } catch (e: IOException) {
        Timber.e(e)
        onError(e)
      } finally {
        hideProcessingProgress()
      }
    }
  }
}

enum class RecordingState {
  UNKNOWN,
  STARTED,
  RESUMED,
  PAUSED,
  STOPPED,
}
