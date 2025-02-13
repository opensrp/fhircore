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

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.util.extension.showToast
import org.smartregister.fhircore.quest.R

@AndroidEntryPoint
class SpeechToTextFragment : Fragment(R.layout.fragment_speech_to_text) {

  private val parentViewModel by activityViewModels<QuestionnaireViewModel>()
  private val viewModel by viewModels<SpeechToTextViewModel>()

  private lateinit var speechInputContainer: View
  private lateinit var speechToEditText: EditText
  private lateinit var processingProgressView: View
  private lateinit var listeningProgressView: View
  private lateinit var progressTextView: TextView
  private lateinit var endButton: View
  private lateinit var resumeButton: Button
  private lateinit var startButton: Button
  private lateinit var pauseButton: Button

  private val processingProgressStringArray: Array<String>
    get() = resources.getStringArray(R.array.processing_progress)

  private var stopRecording: (() -> Unit)? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    speechInputContainer = view.findViewById<View>(R.id.speech_to_text_input_container)
    speechToEditText = view.findViewById<EditText>(R.id.speech_to_text_view)
    processingProgressView = view.findViewById<View>(R.id.processing_progress_view)
    progressTextView = view.findViewById<TextView>(R.id.progress_text_view)
    listeningProgressView = view.findViewById<TextView>(R.id.listening_view)
    endButton = view.findViewById<View>(R.id.end_button)
    startButton = view.findViewById<Button>(R.id.start_button)
    resumeButton = view.findViewById<Button>(R.id.resume_button)
    pauseButton = view.findViewById<Button>(R.id.pause_button)

    speechToEditText.movementMethod = ScrollingMovementMethod()

    startButton.setOnClickListener { startRecording() }
    resumeButton.setOnClickListener { resumeRecording() }

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
      viewModel.speechTranscriptTextStateFlow.collect {
        listeningProgressView.visibility = if (it.isBlank()) View.VISIBLE else View.GONE
        speechInputContainer.visibility = if (it.isNotBlank()) View.VISIBLE else View.GONE
        speechToEditText.setText(it)
      }
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

    startRecording()
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

  override fun onDestroyView() {
    super.onDestroyView()
    stopRecording?.invoke()
  }

  override fun onStop() {
    super.onStop()
    stopRecording?.invoke()
  }

  private fun startRecording() {
    if (
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.RECORD_AUDIO,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      requireContext()
        .showToast(
          getString(R.string.record_audio_denied),
          Toast.LENGTH_SHORT,
        )
      return
    }

    val audioRecord =
      AudioRecord(
        MIC_SOURCE,
        SAMPLE_RATE,
        MIC_CHANNELS,
        MIC_CHANNEL_ENCODING,
        CHUNK_SIZE_SAMPLES * BYTES_PER_SAMPLE,
      )

    val recordingJob = viewModel.startRecording(audioRecord)
    pauseButton.setOnClickListener {
      viewModel.onRecordingPaused()
      viewModel.setCurrentTranscript(speechToEditText.text.toString().trim())
      recordingJob.cancel()
    }
    endButton.setOnClickListener {
      viewModel.onRecordingStopped()
      viewModel.setCurrentTranscript(speechToEditText.text.toString().trim())
      if (recordingJob.isActive) recordingJob.cancel()
      processTranscriptRecorded()
    }
    stopRecording = {
      viewModel.onRecordingStopped()
      if (recordingJob.isActive) recordingJob.cancel()
    }
  }

  private fun resumeRecording() {
    if (
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.RECORD_AUDIO,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      requireContext()
        .showToast(
          getString(R.string.record_audio_denied),
          Toast.LENGTH_SHORT,
        )
      return
    }

    val audioRecord =
      AudioRecord(
        MIC_SOURCE,
        SAMPLE_RATE,
        MIC_CHANNELS,
        MIC_CHANNEL_ENCODING,
        CHUNK_SIZE_SAMPLES * BYTES_PER_SAMPLE,
      )

    viewModel.setCurrentTranscript(speechToEditText.text.toString().trim())

    val recordingJob = viewModel.resumeRecording(audioRecord)
    pauseButton.setOnClickListener {
      viewModel.onRecordingPaused()
      viewModel.setCurrentTranscript(speechToEditText.text.toString().trim())
      recordingJob.cancel()
    }
    endButton.setOnClickListener {
      viewModel.onRecordingStopped()
      viewModel.setCurrentTranscript(speechToEditText.text.toString().trim())
      if (recordingJob.isActive) recordingJob.cancel()
      processTranscriptRecorded()
    }
    stopRecording = {
      viewModel.onRecordingStopped()
      if (recordingJob.isActive) recordingJob.cancel()
    }
  }

  private fun processTranscriptRecorded() {
    speechToEditText.text
      .toString()
      .takeIf { it.isNotBlank() }
      ?.let {
        viewModel.processTranscriptQuestionnaireResponse(
          requireContext(),
          parentViewModel.currentQuestionnaire,
          it,
          { qr -> parentViewModel.showQuestionnaireResponse(qr) },
          { e -> requireContext().showToast(e.message.toString()) },
        )
      }
  }

  companion object {
    private const val MIC_CHANNELS = AudioFormat.CHANNEL_IN_MONO
    private const val MIC_CHANNEL_ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private const val MIC_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
    private const val SAMPLE_RATE = 16000
    private const val CHUNK_SIZE_SAMPLES = 1280
    private const val BYTES_PER_SAMPLE = 2
  }
}
