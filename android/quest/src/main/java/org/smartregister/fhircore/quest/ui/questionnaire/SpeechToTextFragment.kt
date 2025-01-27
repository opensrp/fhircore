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
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.util.extension.decodeResourceFromString
import org.smartregister.fhircore.quest.R

class SpeechToTextFragment : Fragment(R.layout.fragment_speech_to_text) {

  private val viewModel by activityViewModels<QuestionnaireViewModel>()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val speechToTextView = view.findViewById<TextView>(R.id.speech_to_text_view)
    val stopButton = view.findViewById<View>(R.id.stop_button)
    stopButton.setOnClickListener {
      viewModel.showQuestionnaireResponse(testQuestionnaireResponse())
    }
    speechToTextView.movementMethod = ScrollingMovementMethod()

    val resumeButton = view.findViewById<Button>(R.id.resume_button)
    val pauseButton = view.findViewById<Button>(R.id.pause_button)
  }

  private fun testQuestionnaire(): Questionnaire {
    val questionnaire =
      """
      {
        "resourceType": "Questionnaire",
        "id": "dc-clinic-patient-registration",
        "meta": {
          "versionId": "15",
          "lastUpdated": "2025-01-24T11:52:45.124+00:00",
          "source": "#becce910b873b02b",
          "tag": [
            {
              "system": "https://smartregister.org/care-team-tag-id",
              "code": "Not defined",
              "display": "Practitioner CareTeam"
            },
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
              "code": "2.0.0-diabetesCompassClinic",
              "display": "Application Version"
            },
            {
              "system": "https://smartregister.org/practitioner-tag-id",
              "code": "013ddf0d-702a-496d-95d4-06b64f75bdf9",
              "display": "Practitioner"
            }
          ]
        },
        "language": "en-GB",
        "extension": [
          {
            "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap",
            "valueCanonical": "https://fhir.demo.smartregister.org/fhir/StructureMap/dc-clinic-patient-registration-sm"
          },
          {
            "url": "http://hl7.org/fhir/StructureDefinition/variable",
            "valueExpression": {
              "name": "facility-id",
              "language": "text/fhirpath",
              "expression": "%resource.descendants().where(linkId='facility-id').answer.value"
            }
          },
          {
            "url": "http://hl7.org/fhir/StructureDefinition/variable",
            "valueExpression": {
              "name": "isNewRegistration",
              "language": "text/fhirpath",
              "expression": "%resource.descendants().where(linkId='is-edit-profile').answer.value.exists().not() or %resource.descendants().where(linkId='is-edit-profile').answer.value = false"
            }
          }
        ],
        "name": "Diabetes Compass Clinic Patient Registration",
        "title": "Patient Registration",
        "status": "active",
        "subjectType": [
          "Patient"
        ],
        "publisher": "ONA",
        "contact": [
          {
            "name": "https://www.smartregister.org/"
          }
        ],
        "description": "Diabetes Compass Clinic Patient Registration",
        "purpose": "Patient Registration",
        "item": [
          {
            "linkId": "registration-title",
            "prefix": "1.",
            "text": "Registration date",
            "type": "display"
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/entryFormat",
                "valueString": "d/M/yy"
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "today()"
                }
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/maxValue",
                "_valueDate": {
                  "extension": [
                    {
                      "url": "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue",
                      "valueExpression": {
                        "language": "text/fhirpath",
                        "expression": "today()"
                      }
                    }
                  ]
                }
              }
            ],
            "linkId": "enrollment-date",
            "type": "date",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "enrollment-date-hint",
                "text": "Enrollment Date",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                "valueBoolean": true
              }
            ],
            "linkId": "facility-name",
            "type": "string"
          },
          {
            "linkId": "client-information-title",
            "text": "2. Client information",
            "type": "display"
          },
          {
            "extension": [
              {
                "url": "http://ehelse.no/fhir/StructureDefinition/validationtext",
                "valueString": "Enter the correct number of ID number characters"
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/minLength",
                "valueInteger": 10
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.identifier.value[0].toInteger()"
                }
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "flyover",
                      "display": "Fly-over"
                    }
                  ],
                  "text": "Flyover"
                }
              }
            ],
            "linkId": "phn",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.identifier.value",
            "type": "string",
            "required": true,
            "maxLength": 11,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "phn-hint",
                "text": "Personal Health Number",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/minLength",
                "valueInteger": 10
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.identifier.value[1].toInteger()"
                }
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/regex",
                "valueString": "([0-9]{2}[0|1|2|3|5|6|7|8]{1}[0-9]{6}[x|X|v|V])|([0-9]{4}[0|1|2|3|5|6|7|8]{1}[0-9]{7})"
              },
              {
                "url": "http://ehelse.no/fhir/StructureDefinition/validationtext",
                "valueString": "NIC does not match the new or old format"
              }
            ],
            "linkId": "national-identity-number",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.identifier.value",
            "type": "string",
            "required": false,
            "maxLength": 12,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "national-identity-number-hint",
                "text": "NIC Number (Optional)",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                "valueBoolean": true
              }
            ],
            "linkId": "is-edit-profile",
            "type": "boolean"
          },
          {
            "extension": [
              {
                "url": "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                "extension": [
                  {
                    "url": "set-only-readonly",
                    "valueBoolean": false
                  }
                ]
              }
            ],
            "linkId": "qr-code-uuid-widget",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.identifier.value",
            "type": "string",
            "required": false,
            "repeats": false,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "qr-code-uuid-widget-hint",
                "text": "QR Code (Optional)",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-enableWhenExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "%isNewRegistration = false and %resource.descendants().where(linkId='qr-code-uuid-widget').answer.value.exists()"
                }
              },
              {
                "url": "https://github.com/opensrp/android-fhir/StructureDefinition/qr-code-widget",
                "extension": [
                  {
                    "url": "set-only-readonly",
                    "valueBoolean": true
                  }
                ]
              }
            ],
            "linkId": "qr-code-uuid-repeats-widget",
            "type": "string",
            "required": false,
            "repeats": true
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.name.given"
                }
              }
            ],
            "linkId": "reporting-name",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.name.given",
            "type": "string",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "reporting-name-hint",
                "text": "Patient Name",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.birthDate"
                }
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/entryFormat",
                "valueString": "d/M/yy"
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/maxValue",
                "_valueDate": {
                  "extension": [
                    {
                      "url": "http://hl7.org/fhir/StructureDefinition/cqf-calculatedValue",
                      "valueExpression": {
                        "language": "text/fhirpath",
                        "expression": "today() - 15 years"
                      }
                    }
                  ]
                }
              }
            ],
            "linkId": "date-of-birth",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.birthDate",
            "type": "date",
            "enableWhen": [
              {
                "question": "date-of-birth-unknown-choice",
                "operator": "exists",
                "answerBoolean": false
              }
            ],
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-displayCategory",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-display-category",
                          "code": "instructions"
                        }
                      ]
                    }
                  }
                ],
                "linkId": "1-most-recent",
                "text": "Date of birth",
                "type": "display"
              },
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "date-of-birth-hint",
                "text": "Date of birth",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/maxValue",
                "valueInteger": 120
              },
              {
                "url": "http://ehelse.no/fhir/StructureDefinition/sdf-fhirpath",
                "valueString": "Patient.extension.where(url = 'http://helsenorge.no/fhir/StructureDefinition/sdf-age').value"
              },
              {
                "url": "http://hl7.org/fhir/StructureDefinition/minValue",
                "valueInteger": 15
              }
            ],
            "linkId": "age",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.age",
            "type": "integer",
            "enableWhen": [
              {
                "question": "date-of-birth-unknown-choice",
                "operator": "=",
                "answerCoding": {
                  "system": "urn:uuid:d2f70cf6-9320-4f5c-ce52-02f67f2a99e2",
                  "code": "date-of-birth-unknown"
                }
              }
            ],
            "enableBehavior": "all",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "age-hint",
                "text": "Age",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "check-box",
                      "display": "Check-box"
                    }
                  ],
                  "text": "Check-box"
                }
              }
            ],
            "linkId": "date-of-birth-unknown-choice",
            "type": "choice",
            "required": false,
            "repeats": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "urn:uuid:d2f70cf6-9320-4f5c-ce52-02f67f2a99e2",
                  "code": "date-of-birth-unknown",
                  "display": "Unknown DOB, enter age"
                }
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "drop-down",
                      "display": "Drop down"
                    }
                  ],
                  "text": "Drop down"
                }
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.gender"
                }
              }
            ],
            "linkId": "gender",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.gender",
            "type": "choice",
            "required": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "http://hl7.org/fhir/administrative-gender",
                  "code": "male",
                  "display": "Male"
                }
              },
              {
                "valueCoding": {
                  "system": "http://hl7.org/fhir/administrative-gender",
                  "code": "female",
                  "display": "Female"
                }
              },
              {
                "valueCoding": {
                  "system": "http://hl7.org/fhir/administrative-gender",
                  "code": "other",
                  "display": "Other"
                }
              }
            ],
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "gender-hint",
                "text": "Gender",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/minValue",
                "valueInteger": 1
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.telecom.value"
                }
              }
            ],
            "linkId": "phone-number",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.telecom.value",
            "type": "integer",
            "required": false,
            "maxLength": 10,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "phone-number-hint",
                "text": "Phone number (optional)",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "drop-down",
                      "display": "Drop down"
                    }
                  ],
                  "text": "Drop down"
                }
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.communication.language"
                }
              }
            ],
            "linkId": "preferred-language",
            "definition": "http://hl7.org/fhir/StructureDefinition/Patient#Patient.communication.language",
            "type": "choice",
            "required": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "urn:ietf:bcp:47",
                  "code": "si",
                  "display": "Sinhala"
                }
              },
              {
                "valueCoding": {
                  "system": "urn:ietf:bcp:47",
                  "code": "ta",
                  "display": "Tamil"
                }
              },
              {
                "valueCoding": {
                  "system": "urn:ietf:bcp:47",
                  "code": "en",
                  "display": "English"
                }
              }
            ],
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "preferred-language-hint",
                "text": "Preferred SMS text language",
                "type": "display"
              }
            ]
          },
          {
            "linkId": "home-address-title",
            "prefix": "3.",
            "text": "Home address",
            "type": "display"
          },
          {
            "linkId": "address",
            "type": "string",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "address-hint",
                "text": "Address name and house number",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                "valueBoolean": true
              }
            ],
            "linkId": "facility-id",
            "type": "string"
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "drop-down",
                      "display": "Drop down"
                    }
                  ],
                  "text": "Drop down"
                }
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-answerExpression",
                "valueExpression": {
                  "language": "application/x-fhir-query",
                  "expression": "Location?partof=Location/{{%facility-id}}"
                }
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-choiceColumn",
                "extension": [
                  {
                    "url": "path",
                    "valueString": "Location.name"
                  },
                  {
                    "url": "label",
                    "valueString": "name"
                  },
                  {
                    "url": "forDisplay",
                    "valueBoolean": true
                  }
                ]
              }
            ],
            "linkId": "gn-division",
            "type": "reference",
            "enableWhen": [
              {
                "question": "is-outside-catchment-area-choice",
                "operator": "exists",
                "answerBoolean": false
              }
            ],
            "enableBehavior": "all",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "gn-division-hint",
                "text": "GN Division",
                "type": "display"
              }
            ]
          },
          {
            "linkId": "gn-division-out-of-catchment",
            "type": "string",
            "enableWhen": [
              {
                "question": "is-outside-catchment-area-choice",
                "operator": "=",
                "answerCoding": {
                  "system": "urn:uuid:f62eaf4f-9594-476f-b507-1718769c4493",
                  "code": "is-outside-catchment-area"
                }
              }
            ],
            "enableBehavior": "all",
            "required": true,
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "gn-division-out-of-catchment-hint",
                "text": "Out Of Catchment area",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "check-box",
                      "display": "Check-box"
                    }
                  ],
                  "text": "Check-box"
                }
              }
            ],
            "linkId": "is-outside-catchment-area-choice",
            "type": "choice",
            "required": false,
            "repeats": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "urn:uuid:f62eaf4f-9594-476f-b507-1718769c4493",
                  "code": "is-outside-catchment-area",
                  "display": "Patient resides outside catchment area"
                }
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                "valueBoolean": true
              }
            ],
            "linkId": "out-of-catchment-flag-id",
            "type": "string"
          },
          {
            "linkId": "patient-consent-title",
            "prefix": "4.",
            "text": "Patient consent",
            "type": "display",
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-displayCategory",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-display-category",
                          "code": "instructions"
                        }
                      ]
                    }
                  }
                ],
                "linkId": "patient-consent-desc",
                "text": "You confirm that any patient data that you enter has been obtained under appropriate informed consent. This means that the patient or legal guardian know that you are entering personal data into the app, understands what data is being collected, and knows they may be contacted via SMS or other methods using the phone number provided in the app.",
                "type": "display"
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                "valueCodeableConcept": {
                  "coding": [
                    {
                      "system": "http://hl7.org/fhir/questionnaire-item-control",
                      "code": "check-box",
                      "display": "Check-box"
                    }
                  ],
                  "text": "Check-box"
                }
              }
            ],
            "linkId": "patient-consent-choice",
            "type": "choice",
            "required": true,
            "repeats": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "urn:uuid:35926220-b6df-4d08-8554-8d3000f37d67",
                  "code": "patient-consent",
                  "display": "Patient gives consent"
                }
              }
            ],
            "item": [
              {
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    "valueCodeableConcept": {
                      "coding": [
                        {
                          "system": "http://hl7.org/fhir/questionnaire-item-control",
                          "code": "flyover",
                          "display": "Fly-over"
                        }
                      ],
                      "text": "Flyover"
                    }
                  }
                ],
                "linkId": "patient-consent-choice-hint",
                "text": "Patient gives consent",
                "type": "display"
              }
            ]
          }
        ]
      }
        """
        .trimIndent()
    return questionnaire.decodeResourceFromString()
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
