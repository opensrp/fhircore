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

package org.smartregister.fhircore.quest.medintel.speech.speechtoform

import android.media.AudioFormat
import android.media.AudioRecord
import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1p1beta1.RecognitionConfig
import com.google.cloud.speech.v1p1beta1.SpeechClient
import com.google.cloud.speech.v1p1beta1.SpeechSettings
import com.google.cloud.speech.v1p1beta1.StreamingRecognitionConfig
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeRequest
import com.google.cloud.speech.v1p1beta1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.quest.BuildConfig
import timber.log.Timber

class LiveSpeechToText @Inject constructor(val dispatcherProvider: DispatcherProvider) {

  private val sampleRate = 16000
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

  private val speechSettings: SpeechSettings by lazy {
    SpeechSettings.newBuilder().setApiKey(BuildConfig.SPEECH_TO_TEXT_API_KEY).build()
  }

  fun startTranscription(audioRecord: AudioRecord): Flow<String> {
    return callbackFlow<String> {
        val speechClient = SpeechClient.create(speechSettings)
        val responseObserver =
          object : ResponseObserver<StreamingRecognizeResponse> {
            override fun onStart(controller: StreamController) {
              // No-op
            }

            override fun onResponse(response: StreamingRecognizeResponse) {
              response.resultsList
                .flatMap { it.alternativesList }
                .forEach {
                  val transcript = it.transcript
                  trySend(transcript)
                  Timber.i("Transcription: $transcript")
                }
            }

            override fun onError(t: Throwable) {
              t.printStackTrace()
              Timber.e("Error during streaming: ${t.message}")
              cancel(CancellationException("Streaming Error", t))
            }

            override fun onComplete() {
              Timber.i("Streaming completed.")
              channel.close()
            }
          }

        val requestObserver: ClientStream<StreamingRecognizeRequest> =
          speechClient.streamingRecognizeCallable().splitCall(responseObserver)

        val recognitionConfig =
          RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(sampleRate)
            .setLanguageCode("en-US")
            .build()

        val streamingRecognitionConfig =
          StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build()

        val configRequest =
          StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(streamingRecognitionConfig)
            .build()

        requestObserver.send(configRequest)

        audioRecord.startRecording()
        val audioData = ByteArray(bufferSize)
        var isRecording = true

        CoroutineScope(dispatcherProvider.io()).launch {
          while (isRecording) {
            val read = audioRecord.read(audioData, 0, bufferSize)
            // TODO we want the caller to control stopping this loop, not on no data
            if (read > 0) {
              val audioBytes = ByteString.copyFrom(audioData, 0, read)
              val audioRequest =
                StreamingRecognizeRequest.newBuilder().setAudioContent(audioBytes).build()
              requestObserver.send(audioRequest)
            } else {
              isRecording = false
            }
          }

          requestObserver.closeSend()
          speechClient.close()
          audioRecord.stop()
          audioRecord.release()
        }

        awaitClose { isRecording = false }
      }
      .flowOn(dispatcherProvider.io())
  }
}
