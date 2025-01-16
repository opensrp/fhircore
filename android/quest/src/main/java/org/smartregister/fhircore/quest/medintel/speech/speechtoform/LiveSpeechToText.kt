package org.smartregister.fhircore.quest.medintel.speech.speechtoform

import android.media.AudioFormat
import android.media.AudioRecord
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.protobuf.ByteString
import java.util.logging.Logger
import com.google.api.gax.rpc.ClientStream
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse

class LiveSpeechToText {

  private val logger = Logger.getLogger(LiveSpeechToText::class.java.name)
  private val sampleRate = 16000
  private val channelConfig = AudioFormat.CHANNEL_IN_MONO
  private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
  private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

  fun startTranscription(audioRecord: AudioRecord) {
    SpeechClient.create().use { speechClient ->
      val responseObserver = object : ResponseObserver<StreamingRecognizeResponse> {
        override fun onStart(controller: StreamController) {
          // No-op
        }

        override fun onResponse(response: StreamingRecognizeResponse) {
          // TODO we want to emit this back to the caller, not log it
          for (result in response.resultsList) {
            for (alternative in result.alternativesList) {
              logger.info("Transcription: ${alternative.transcript}")
            }
          }
        }

        override fun onError(t: Throwable) {
          logger.severe("Error during streaming: ${t.message}")
        }

        override fun onComplete() {
          logger.info("Streaming completed.")
        }
      }

      val requestObserver: ClientStream<StreamingRecognizeRequest> =
        speechClient.streamingRecognizeCallable().splitCall(responseObserver)

      val recognitionConfig = RecognitionConfig.newBuilder()
        .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
        .setSampleRateHertz(sampleRate)
        .setLanguageCode("en-US")
        .build()

      val streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
        .setConfig(recognitionConfig)
        .build()

      val configRequest = StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(streamingRecognitionConfig)
        .build()

      requestObserver.send(configRequest)

      audioRecord.startRecording()
      val audioData = ByteArray(bufferSize)
      var isRecording = true
      while (isRecording) {
        val read = audioRecord.read(audioData, 0, bufferSize)
        // TODO we want the caller to control stopping this loop, not on no data
        if (read > 0) {
          val audioBytes = ByteString.copyFrom(audioData, 0, read)
          val audioRequest = StreamingRecognizeRequest.newBuilder()
            .setAudioContent(audioBytes)
            .build()
          requestObserver.send(audioRequest)
        } else {
          isRecording = false
        }
      }
      requestObserver.closeSend()
    }
  }
}