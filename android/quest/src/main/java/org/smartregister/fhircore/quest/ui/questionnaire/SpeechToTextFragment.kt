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

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.quest.R
import org.smartregister.fhircore.quest.medintel.speech.speechtoform.TextToForm

class SpeechToTextFragment : Fragment(R.layout.fragment_speech_to_text) {

  private val parentViewModel by activityViewModels<QuestionnaireViewModel>()
  private val viewModel by viewModels<SpeechToTextViewModel>()

  private lateinit var speechInputContainer: View
  private lateinit var speechToTextView: TextView
  private lateinit var processingProgressView: View
  private lateinit var progressTextView: TextView
  private lateinit var stopButton: View
  private lateinit var resumeButton: Button
  private lateinit var startButton: Button
  private lateinit var pauseButton: Button

  private val processingProgressStringArray: Array<String>
    get() = resources.getStringArray(R.array.processing_progress)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    speechInputContainer = view.findViewById<View>(R.id.speech_to_text_input_container)
    speechToTextView = view.findViewById<TextView>(R.id.speech_to_text_view)
    processingProgressView = view.findViewById<View>(R.id.processing_progress_view)
    progressTextView = view.findViewById<TextView>(R.id.progress_text_view)
    stopButton = view.findViewById<View>(R.id.stop_button)
    startButton = view.findViewById<Button>(R.id.start_button)
    resumeButton = view.findViewById<Button>(R.id.resume_button)
    pauseButton = view.findViewById<Button>(R.id.pause_button)
    speechToTextView.movementMethod = ScrollingMovementMethod()

    startButton.setOnClickListener { startRecording() }
    resumeButton.setOnClickListener { resumeRecording() }
    pauseButton.setOnClickListener { pauseRecording() }
    stopButton.setOnClickListener { processTranscriptRecorded() }

    lifecycleScope.launch {
      var processingProgressJob: Job? = null
      viewModel.showProcessingProgressStateFlow.collect {
        if (it) {
          processingProgressView.visibility = View.VISIBLE
          speechInputContainer.visibility = View.GONE
          processingProgressJob = renderProcessingProgressView()
        } else {
          processingProgressJob?.cancel()
          processingProgressJob = null
          processingProgressView.visibility = View.GONE
        }
      }
    }

    lifecycleScope.launch {
      viewModel.speechTranscriptTextStateFlow.collect { speechToTextView.text = it }
    }

    lifecycleScope.launch {
      viewModel.recordingStateFlow.collect {
        when (it) {
          RecordingState.UNKNOWN,
          RecordingState.STOPPED, -> {
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.GONE
            startButton.visibility = View.VISIBLE
          }
          RecordingState.STARTED,
          RecordingState.RESUMED, -> {
            pauseButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            startButton.visibility = View.GONE
          }
          RecordingState.PAUSED -> {
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.VISIBLE
            startButton.visibility = View.GONE
          }
        }
      }
    }
  }

  private fun renderProcessingProgressView(): Job {
    return lifecycleScope.launch {
      flow {
          var counter = 0
          while (true) {
            emit(counter)
            delay(500)
            counter++
          }
        }
        .collect {
          progressTextView.text =
            processingProgressStringArray[it % processingProgressStringArray.size]
        }
    }
  }

  override fun onPause() {
    super.onPause()
    pauseRecording()
  }

  override fun onStop() {
    super.onStop()
    stopRecording()
  }

  private fun startRecording() {
    viewModel.resetTranscript()
  }

  private fun resumeRecording() {}

  private fun pauseRecording() {
    viewModel.onRecordingPaused()
  }

  private fun stopRecording() {
    viewModel.onRecordingStopped()
  }

  private fun processTranscriptRecorded() {
    lifecycleScope.launch {
      speechToTextView.text
        .toString()
        .takeIf { it.isNotBlank() }
        ?.let { processTranscriptQuestionnaireResponse(it) }
    }
  }

  private suspend fun processTranscriptQuestionnaireResponse(transcript: String) {
    viewModel.showProcessingProgress()
    val result =
      TextToForm.generateQuestionnaireResponse(
        transcript,
        parentViewModel.currentQuestionnaire,
        requireActivity(),
        viewModel.geminiModel,
      )
    viewModel.hideProcessingProgress()
    parentViewModel.showQuestionnaireResponse(result)
  }
}
