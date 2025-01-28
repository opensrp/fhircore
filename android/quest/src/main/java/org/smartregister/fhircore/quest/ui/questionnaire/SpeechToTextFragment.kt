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
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.quest.R

class SpeechToTextFragment : Fragment(R.layout.fragment_speech_to_text) {

  private val viewModel by activityViewModels<QuestionnaireViewModel>()
  private lateinit var speechInputContainer: View
  private lateinit var speechToTextView: TextView
  private lateinit var processingProgressView: View
  private lateinit var progressTextView: TextView
  private lateinit var stopButton: View
  private lateinit var resumeButton: Button
  private lateinit var pauseButton: Button

  private var showProgress: Boolean = false

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    speechInputContainer = view.findViewById<View>(R.id.speech_to_text_input_container)
    speechToTextView = view.findViewById<TextView>(R.id.speech_to_text_view)
    processingProgressView = view.findViewById<View>(R.id.processing_progress_view)
    progressTextView = view.findViewById<TextView>(R.id.progress_text_view)
    stopButton = view.findViewById<View>(R.id.stop_button)
    resumeButton = view.findViewById<Button>(R.id.resume_button)
    pauseButton = view.findViewById<Button>(R.id.pause_button)

    stopButton.setOnClickListener {
      showProcessingProgress()
      transformTextToQuestionnaireResponse()
    }
    speechToTextView.movementMethod = ScrollingMovementMethod()
  }

  private fun showProcessingProgress() {
    processingProgressView.visibility = View.VISIBLE
    speechInputContainer.visibility = View.GONE

    showProgress = true
    val progressStrings = resources.getStringArray(R.array.processing_progress)
    lifecycleScope.launch {
      var counter = 0
      while (showProgress) {
        progressTextView.text = progressStrings[counter % progressStrings.size]
        counter++
        delay(500)
      }
    }
  }

  private fun transformTextToQuestionnaireResponse() {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(
      {
        processingProgressView.visibility = View.GONE
        showProgress = false
        viewModel.showQuestionnaireResponse(testQuestionnaireResponse())
      },
      3500,
    )
  }

  private fun testQuestionnaireResponse(): QuestionnaireResponse {
    val qrString =
      """
      {
        "resourceType": "QuestionnaireResponse",
        "id": "031df7fc-72c2-4bbf-8242-cd7f224d4fa7",
        "meta": {
          "versionId": "1",
          "lastUpdated": "2025-01-24T11:55:46.733+00:00",
          "source": "#e34d8f6ce562cbe7",
          "tag": [
            {
              "system": "https://smartregister.org/care-team-tag-id",
              "code": "451d9442-f6c0-4ff0-b081-f69bb360fd08",
              "display": "Practitioner CareTeam"
            },
            {
              "system": "https://smartregister.org/organisation-tag-id",
              "code": "e79d4407-2618-4561-8664-76eee93bc2b2",
              "display": "Practitioner Organization"
            },
            {
              "system": "https://smartregister.org/practitioner-tag-id",
              "code": "a21decb7-ac90-43e1-926a-0579a46816ed",
              "display": "Practitioner"
            },
            {
              "system": "https://smartregister.org/location-tag-id",
              "code": "PKT0010322",
              "display": "Practitioner Location"
            },
            {
              "system": "https://smartregister.org/app-version",
              "code": "2.1.0-diabetesCompassClinic",
              "display": "Application Version"
            }
          ]
        },
        "extension": [
          {
            "url": "http://github.com/google-android/questionnaire-lastLaunched-timestamp",
            "valueDateTime": "2025-01-20T14:58:20+03:00"
          }
        ],
        "questionnaire": "Questionnaire/dc-clinic-patient-registration",
        "status": "completed",
        "authored": "2025-01-20T14:58:55+03:00",
        "item": [
          {
            "linkId": "registration-title",
            "text": "Registration date"
          },
          {
            "linkId": "enrollment-date",
            "answer": [
              {
                "valueDate": "2025-01-20",
                "item": [
                  {
                    "linkId": "enrollment-date-hint",
                    "text": "Enrollment Date"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "facility-name",
            "answer": [
              {
                "valueString": "Baduraliya health facility"
              }
            ]
          },
          {
            "linkId": "client-information-title",
            "text": "2. Client information"
          },
          {
            "linkId": "phn",
            "answer": [
              {
                "valueString": "1234567890",
                "item": [
                  {
                    "linkId": "phn-hint",
                    "text": "Personal Health Number"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "national-identity-number",
            "answer": [
              {
                "valueString": "006555555x",
                "item": [
                  {
                    "linkId": "national-identity-number-hint",
                    "text": "NIC Number (Optional)"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "is-edit-profile",
            "answer": [
              {
                "valueBoolean": true
              }
            ]
          },
          {
            "linkId": "qr-code-uuid-widget"
          },
          {
            "linkId": "reporting-name",
            "answer": [
              {
                "valueString": "Joshua Makau",
                "item": [
                  {
                    "linkId": "reporting-name-hint",
                    "text": "Patient Name"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "date-of-birth",
            "answer": [
              {
                "valueDate": "2000-01-20",
                "item": [
                  {
                    "linkId": "1-most-recent",
                    "text": "Date of birth"
                  },
                  {
                    "linkId": "date-of-birth-hint",
                    "text": "Date of birth"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "date-of-birth-unknown-choice"
          },
          {
            "linkId": "gender",
            "answer": [
              {
                "valueCoding": {
                  "system": "http://hl7.org/fhir/administrative-gender",
                  "code": "male",
                  "display": "Male"
                },
                "item": [
                  {
                    "linkId": "gender-hint",
                    "text": "Gender"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "phone-number"
          },
          {
            "linkId": "preferred-language",
            "answer": [
              {
                "valueCoding": {
                  "system": "urn:ietf:bcp:47",
                  "code": "en",
                  "display": "English"
                },
                "item": [
                  {
                    "linkId": "preferred-language-hint",
                    "text": "Preferred SMS text language"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "home-address-title",
            "text": "Home address"
          },
          {
            "linkId": "address",
            "answer": [
              {
                "valueString": "123",
                "item": [
                  {
                    "linkId": "address-hint",
                    "text": "Address name and house number"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "facility-id",
            "answer": [
              {
                "valueString": "PKT0010322"
              }
            ]
          },
          {
            "linkId": "gn-division",
            "answer": [
              {
                "valueReference": {
                  "reference": "Location/836M",
                  "display": "Ambegoda"
                },
                "item": [
                  {
                    "linkId": "gn-division-hint",
                    "text": "GN Division"
                  }
                ]
              }
            ]
          },
          {
            "linkId": "is-outside-catchment-area-choice"
          },
          {
            "linkId": "out-of-catchment-flag-id"
          },
          {
            "linkId": "patient-consent-title",
            "text": "Patient consent"
          },
          {
            "linkId": "patient-consent-choice",
            "answer": [
              {
                "valueCoding": {
                  "system": "urn:uuid:35926220-b6df-4d08-8554-8d3000f37d67",
                  "code": "patient-consent",
                  "display": "Patient gives consent"
                },
                "item": [
                  {
                    "linkId": "patient-consent-choice-hint",
                    "text": "Patient gives consent"
                  }
                ]
              }
            ]
          }
        ]
      }
        """
        .trimIndent()
    return qrString.decodeResourceFromString()
  }
}
