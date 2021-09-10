/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.eir.robolectric.RobolectricTest
import org.smartregister.fhircore.eir.shadow.EirApplicationShadow
import org.smartregister.fhircore.engine.util.helper.TransformSupportServices

/**
 * Provides a playground for quickly testing and authoring questionnaire and the respective
 * StructureMap
 *
 * This should be removed at a later point once we have a more clear way of doing this
 */
@Config(shadows = [EirApplicationShadow::class])
class StructureMapTestUtilities : RobolectricTest() {

  @Test
  fun testPopulation() {
    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val questionnaire = iParser.parseResource(Questionnaire::class.java, questionnaire)
    val patient = iParser.parseResource(Patient::class.java, PATIENT_JSON)
    val relatedPerson = iParser.parseResource(RelatedPerson::class.java, RELATED_PESRON_JSON)

    var questionnaireResponse: QuestionnaireResponse
    runBlocking {
      questionnaireResponse = ResourceMapper.populate(questionnaire, patient, relatedPerson)
    }

    val expected =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt u"

    /*try {
      val `is` = RuntimeEnvironment.application
          .assets
          .open("registration_structuremap.txt")
      val size: Int = `is`.available()
      val buffer = ByteArray(size)
      `is`.read(buffer)
      `is`.close()
      val fileContents = String(buffer, Charset.forName("UTF-8"))


    } catch (e: IOException) {
      e.printStackTrace()
    }*/

    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(outputs, contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(fhirMapToConvert, "PatientRegistration")
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    scu.transform(contextR4, questionnaireResponse, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Test
  fun convertFhirMapToJson() {
    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))
    contextR4.isCanRunWithoutTerminology = true

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4)
    val map = scu.parse(fhirMapToConvert, "PatientRegistration")

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)
  }

  @Test
  fun performExtraction() {
    val pcm = FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION)
    // Package name manually checked from
    // https://simplifier.net/packages?Text=hl7.fhir.core&fhirVersion=All+FHIR+Versions
    val contextR4 = SimpleWorkerContext.fromPackage(pcm.loadPackage("hl7.fhir.r4.core", "4.0.1"))

    contextR4.setExpansionProfile(Parameters())
    contextR4.isCanRunWithoutTerminology = true

    val outputs: MutableList<Base> = ArrayList()
    val transformSupportServices = TransformSupportServices(outputs, contextR4)

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4, transformSupportServices)
    val map = scu.parse(fhirMapToConvert, "PatientRegistration")

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    val targetResource = Bundle()

    val baseElement =
      iParser.parseResource(QuestionnaireResponse::class.java, questionnaireResponse)

    scu.transform(contextR4, baseElement, map, targetResource)

    System.out.println(iParser.encodeResourceToString(targetResource))
  }

  @Language("JSON")
  private val questionnaire =
    """
      {
        "resourceType": "Questionnaire",
        "id": "1452",
        "meta": {
          "versionId": "2",
          "lastUpdated": "2021-08-16T22:41:37.890+00:00",
          "source": "#SD4iNdKeaQb71nEF"
        },
        "extension": [
          {
            "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap",
            "valueCanonical": "https://fhir.labs.smartregister.org/StructureMap/1453"
          }
        ],
        "status": "active",
        "subjectType": [
          "Patient"
        ],
        "date": "2020-11-18T07:24:47.111Z",
        "item": [
          {
            "linkId": "patient-barcode",
            "text": "Barcode",
            "type": "text",
            "extension": [
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "Patient.id"
                }
              }
            ]
          },
          {
            "linkId": "PR",
            "text": "Client Info",
            "_text": {
              "extension": [
                {
                  "url": "http://hl7.org/fhir/StructureDefinition/translation",
                  "extension": [
                    {
                      "url": "lang",
                      "valueCode": "sw"
                    },
                    {
                      "url": "content",
                      "valueString": "Maelezo ya mteja"
                    }
                  ]
                }
              ]
            },
            "type": "group",
            "item": [
              {
                "linkId": "PR-name",
                "type": "group",
                "item": [
                  {
                    "linkId": "PR-name-given",
                    "text": "First Name",
                    "_text": {
                      "extension": [
                        {
                          "url": "http://hl7.org/fhir/StructureDefinition/translation",
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Jina la kwanza"
                            }
                          ]
                        }
                      ]
                    },
                    "extension": [
                      {
                        "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                        "valueExpression": {
                          "name": "patientName",
                          "language": "text/fhirpath",
                          "expression": "Patient.name.given"
                        }
                      }
                    ],
                    "type": "string",
                    "required": true
                  },
                  {
                    "linkId": "PR-name-family",
                    "text": "Family Name",
                    "_text": {
                      "extension": [
                        {
                          "url": "http://hl7.org/fhir/StructureDefinition/translation",
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Jina la ukoo"
                            }
                          ]
                        }
                      ]
                    },
                    "type": "string",
                    "required": true,
                    "extension": [
                      {
                        "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                        "valueExpression": {
                          "name": "patientFamily",
                          "language": "text/fhirpath",
                          "expression": "Patient.name.family"
                        }
                      }
                    ]
                  }
                ]
              },
              {
                "linkId": "patient-0-birth-date",
                "text": "Date of Birth",
                "_text": {
                  "extension": [
                    {
                      "url": "http://hl7.org/fhir/StructureDefinition/translation",
                      "extension": [
                        {
                          "url": "lang",
                          "valueCode": "sw"
                        },
                        {
                          "url": "content",
                          "valueString": "Tarehe ya kuzaliwa"
                        }
                      ]
                    }
                  ]
                },
                "type": "date",
                "required": true,
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "name": "patientBirthDate",
                      "language": "text/fhirpath",
                      "expression": "Patient.birthDate"
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
                          "code": "radio-button",
                          "display": "Radio Button"
                        }
                      ],
                      "text": "A control where choices are listed with a button beside them. The button can be toggled to select or de-select a given choice. Selecting one item deselects all others."
                    }
                  },
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-choiceOrientation",
                    "valueCode": "horizontal"
                  },
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "name": "patientGender",
                      "language": "text/fhirpath",
                      "expression": "Patient.gender"
                    }
                  }
                ],
                "linkId": "patient-0-gender",
                "text": "Gender",
                "type": "choice",
                "answerOption": [
                  {
                    "valueCoding": {
                      "code": "female",
                      "display": "Female"
                    }
                  },
                  {
                    "valueCoding": {
                      "code": "male",
                      "display": "Male"
                    }
                  }
                ],
                "initial": [
                  {
                    "valueCoding": {
                      "code": "female",
                      "display": "Female"
                    }
                  }
                ]
              },
              {
                "linkId": "PR-telecom",
                "type": "group",
                "item": [
                  {
                    "linkId": "PR-telecom-system",
                    "text": "system",
                    "type": "string",
                    "enableWhen": [
                      {
                        "question": "patient-0-gender",
                        "operator": "=",
                        "answerString": "ok"
                      }
                    ],
                    "initial": [
                      {
                        "valueString": "phone"
                      }
                    ]
                  },
                  {
                    "linkId": "PR-telecom-value",
                    "text": "Phone Number",
                    "extension": [
                      {
                        "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                        "valueExpression": {
                          "name": "patientTelecom",
                          "language": "text/fhirpath",
                          "expression": "Patient.telecom.value"
                        }
                      }
                    ],
                    "_text": {
                      "extension": [
                        {
                          "url": "http://hl7.org/fhir/StructureDefinition/translation",
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Nambari ya simu"
                            }
                          ]
                        }
                      ]
                    },
                    "type": "string",
                    "required": true
                  }
                ]
              },
              {
                "linkId": "PR-address",
                "type": "group",
                "item": [
                  {
                    "linkId": "PR-address-city",
                    "text": "City",
                    "extension": [
                      {
                        "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                        "valueExpression": {
                          "name": "patientCity",
                          "language": "text/fhirpath",
                          "expression": "Patient.address.city"
                        }
                      }
                    ],
                    "_text": {
                      "extension": [
                        {
                          "url": "http://hl7.org/fhir/StructureDefinition/translation",
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Mji"
                            }
                          ]
                        }
                      ]
                    },
                    "type": "string"
                  },
                  {
                    "linkId": "PR-address-country",
                    "text": "Country",
                    "_text": {
                      "extension": [
                        {
                          "url": "http://hl7.org/fhir/StructureDefinition/translation",
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Nchi"
                            }
                          ]
                        }
                      ]
                    },
                    "extension": [
                      {
                        "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                        "valueExpression": {
                          "name": "patientCountry",
                          "language": "text/fhirpath",
                          "expression": "Patient.address.country"
                        }
                      }
                    ],
                    "type": "string"
                  }
                ]
              },
              {
                "linkId": "PR-active",
                "text": "Is Active?",
                "_text": {
                  "extension": [
                    {
                      "url": "http://hl7.org/fhir/StructureDefinition/translation",
                      "extension": [
                        {
                          "url": "lang",
                          "valueCode": "sw"
                        },
                        {
                          "url": "content",
                          "valueString": "Inatumika?"
                        }
                      ]
                    }
                  ]
                },
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "name": "patientActive",
                      "language": "text/fhirpath",
                      "expression": "Patient.active"
                    }
                  }
                ],
                "type": "boolean"
              }
            ]
          },
          {
            "linkId": "RP",
            "text": "Related person",
            "type": "group",
            "item": [
              {
                "linkId": "RP-id",
                "text": "Related person id",
                "type": "text",
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                    "valueBoolean": true
                  },
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.id"
                    }
                  }
                ]
              },
              {
                "linkId": "RP-family-name",
                "text": "Family name",
                "type": "text",
                "required": true,
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.name.family"
                    }
                  }
                ]
              },
              {
                "linkId": "RP-first-name",
                "text": "First name",
                "type": "text",
                "required": true,
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.name.given"
                    }
                  }
                ]
              },
              {
                "linkId": "RP-relationship",
                "text": "Relationship to patient",
                "type": "text",
                "required": true,
                "answerValueSet": "http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype",
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.relationship.coding.code"
                    }
                  }
                ]
              },
              {
                "linkId": "RP-contact-1",
                "text": "Phone number",
                "type": "text",
                "required": true,
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.telecom[0].value"
                    }
                  }
                ]
              },
              {
                "linkId": "RP-contact-alternate",
                "text": "Alternative phone number",
                "type": "text",
                "extension": [
                  {
                    "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                    "valueExpression": {
                      "language": "text/fhirpath",
                      "expression": "RelatedPerson.telecom[0].value"
                    }
                  }
                ]
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
                      "code": "check-box"
                    }
                  ]
                }
              }
            ],
            "linkId": "comorbidities",
            "code": [
              {
                "system": "https://www.snomed.org",
                "code": "991381000000107"
              }
            ],
            "text": "Do you have any of the following conditions?",
            "type": "choice",
            "required": true,
            "repeats": true,
            "answerOption": [
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "73211009",
                  "display": "Diabetes Mellitus (DM)"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "59621000",
                  "display": "HyperTension (HT)"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "414545008",
                  "display": "Ischemic Heart Disease (IHD / CHD / CCF)"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "56717001",
                  "display": "Tuberculosis (TB)"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "195967001",
                  "display": "Asthma/COPD"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "709044004",
                  "display": "Chronic Kidney Disease"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "363346000",
                  "display": "Cancer"
                }
              },
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "74964007",
                  "display": "Others"
                }
              }
            ]
          },
          {
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/RiskAssessment",
                "valueBoolean": true
              }
            ],
            "linkId": "other_comorbidities",
            "code": [
              {
                "system": "https://www.snomed.org",
                "code": "38651000000103"
              }
            ],
            "text": "If other, specify: ",
            "type": "string",
            "enableWhen": [
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "74964007",
                  "display": "Others"
                }
              }
            ],
            "enableBehavior": "all"
          },
          {
            "linkId": "risk_assessment",
            "code": [
              {
                "system": "https://www.snomed.org",
                "code": "225338004",
                "display": "Risk Assessment"
              }
            ],
            "text": "Client is at risk for serious illness from COVID-19",
            "type": "choice",
            "enableWhen": [
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "74964007",
                  "display": "Others"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "363346000",
                  "display": "Cancer"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "709044004",
                  "display": "Chronic Kidney Disease"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "195967001",
                  "display": "Asthma/COPD"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "56717001",
                  "display": "Tuberculosis (TB)"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "414545008",
                  "display": "Ischemic Heart Disease (IHD / CHD / CCF)"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "59621000",
                  "display": "HyperTension (HT)"
                }
              },
              {
                "question": "comorbidities",
                "operator": "=",
                "answerCoding": {
                  "system": "https://www.snomed.org",
                  "code": "73211009",
                  "display": "Diabetes Mellitus (DM)"
                }
              }
            ],
            "enableBehavior": "any",
            "initial": [
              {
                "valueCoding": {
                  "system": "https://www.snomed.org",
                  "code": "870577009",
                  "display": "High Risk for COVID-19"
                }
              }
            ]
          },
          {
            "linkId": "RP-id",
            "text": "Related person id",
            "type": "text",
            "extension": [
              {
                "url": "http://hl7.org/fhir/StructureDefinition/questionnaire-hidden",
                "valueBoolean": true
              },
              {
                "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression",
                "valueExpression": {
                  "language": "text/fhirpath",
                  "expression": "RelatedPerson.id.value"
                }
              }
            ]
          }
        ]
      }
    """.trimIndent()

  @Language("JSON")
  private val questionnaireResponse =
    """
    {
      "resourceType": "QuestionnaireResponse",
      "questionnaire": "Questionnaire/client-registration-sample",
      "item": [
        {
          "linkId": "PR",
          "item": [
            {
              "linkId": "PR-name",
              "item": [
                {
                  "linkId": "PR-name-given",
                  "answer": [
                    {
                      "valueString": "Mike"
                    }
                  ]
                },
                {
                  "linkId": "PR-name-family",
                  "answer": [
                    {
                      "valueString": "Doe"
                    }
                  ]
                }
              ]
            },
            {
              "linkId": "patient-0-birth-date",
              "answer": [
                {
                  "valueDate": "2021-07-01"
                }
              ]
            },
            {
              "linkId": "patient-0-gender",
              "answer": [
                {
                  "valueCoding": {
                    "code": "male",
                    "display": "Male"
                  }
                }
              ]
            },
            {
              "linkId": "PR-telecom",
              "item": [
                {
                  "linkId": "PR-telecom-system",
                  "answer": [
                    {
                      "valueString": "phone"
                    }
                  ]
                },
                {
                  "linkId": "PR-telecom-value",
                  "answer": [
                    {
                      "valueString": "0700 000 000"
                    }
                  ]
                }
              ]
            },
            {
              "linkId": "PR-address",
              "item": [
                {
                  "linkId": "PR-address-city",
                  "answer": [
                    {
                      "valueString": "Nairobi"
                    }
                  ]
                },
                {
                  "linkId": "PR-address-country",
                  "answer": [
                    {
                      "valueString": "Kenya"
                    }
                  ]
                }
              ]
            },
            {
              "linkId": "PR-active",
              "answer": [
                {
                  "valueBoolean": true
                }
              ]
            }
          ]
        },
        {
          "linkId": "RP",
          "item": [
            {
              "linkId": "RP-family-name",
              "answer": [
                {
                  "valueString": "Doe"
                }
              ]
            },
            {
              "linkId": "RP-first-name",
              "answer": [
                {
                  "valueString": "Mama-mike"
                }
              ]
            },
            {
              "linkId": "RP-relationship",
              "answer": [
                {
                  "valueString": "PRN"
                }
              ]
            },
            {
              "linkId": "RP-contact-1",
              "answer": [
                {
                  "valueString": "0700 001 001"
                }
              ]
            },
            {
              "linkId": "RP-contact-alternate",
              "answer": [
                {
                  "valueString": "0700 000 012"
                }
              ]
            }
          ]
        },
        {
          "linkId": "comorbidities",
          "answer": [
            {
              "valueCoding": {
                "display": "Cancer",
                "system": "https://www.snomed.org",
                "code": "363346000"
              }
            },
            {
              "valueCoding": {
                "display": "Others",
                "system": "https://www.snomed.org",
                "code": "74964007"
              }
            }
          ]
        },
        {
          "linkId": "other_comorbidities",
          "answer": [
            {
              "valueString": "This is another comorbidity"
            }
          ]
        },
        {
          "linkId": "patient-barcode",
          "answer": [
            {
              "valueString": "scanned-barcode-string"
            }
          ]
        },
        {
          "linkId": "RP-id"
        }
      ]
    }
    """.trimIndent()

  private val fhirMapToConvert =
    """
    map "http://hl7.org/fhir/StructureMap/PatientRegistration" = 'PatientRegistration'

uses "http://hl7.org/fhir/StructureDefinition/QuestionnaireReponse" as source
uses "http://hl7.org/fhir/StructureDefinition/Bundle" as target
uses "http://hl7.org/fhir/StructureDefinition/Patient" as target
uses "http://hl7.org/fhir/StructureDefinition/Observation" as target
uses "http://hl7.org/fhir/StructureDefinition/Patient" as source

group PatientRegistration(source src : QuestionnaireResponse, target bundle: Bundle) {
    src -> bundle.id = uuid() "rule_c";
    src -> bundle.type = 'collection' "rule_b";
    src -> bundle.entry as entry, entry.resource = create('Patient') as patient then
        ExtractPatient(src, patient), ExtractRelatedPerson(src, bundle, patient) "rule_a";
    
    src.item as relatedPersonIdItem where(linkId = 'RP-id' and answer.count() = 0) then {
      src -> evaluate(bundle, ${"$"}this.entry.resource.where(${"$"}this.is(Patient))) as patient then ExtractObservations(src, bundle, patient), ExtractRiskAssessmentObservation(src, bundle, bundle, patient) "rule_d";
    } "rule_e";
}

group ExtractPatient(source src : QuestionnaireResponse, target patient : Patient) {
    src -> patient.id = uuid() "rule_patient_id_generation";
	src.item as patientBarcodeItem where(linkId = 'patient-barcode' and answer.count() > 0 and answer.empty().not()) then {
          src -> patient.id = create('id') as patientId then {
            src -> patientId.value = evaluate(patientBarcodeItem, ${"$"}this.answer.value) "rule_j1_1";
          } "rule_j1_2";
    };

    src.item as item where(linkId = 'PR') then {
       item.item as inner_item where (linkId = 'patient-0-birth-date') then {
           inner_item.answer first as ans then { 
               ans.value as val -> patient.birthDate = val "rule_a";
           };
       };
       
       item.item as nameItem where(linkId = 'PR-name') -> patient.name = create('HumanName') as patientName then {  
          src -> patientName.family = evaluate(nameItem, ${"$"}this.item.where(linkId = 'PR-name-family').answer.value) "rule_d";
          src -> patientName.given = evaluate(nameItem, ${"$"}this.item.where(linkId = 'PR-name-given').answer.value) "rule_e";
       };

       src -> patient.gender = evaluate(item, ${"$"}this.item.where(linkId = 'patient-0-gender').answer.value.code) "rule_f";
       item.item as telecomBlock where (linkId = 'PR-telecom') -> patient.telecom = create('ContactPoint') as patientContact then {
          src -> patientContact.value = evaluate(telecomBlock, ${"$"}this.item.where(linkId = 'PR-telecom-value').answer.value) "rule_f1";
          src -> patientContact.system = "phone" "rule_f2";
          src -> patientContact.rank = create('positiveInt') as posInt then {
            src -> posInt.value = "1" "rule_f5";
          } "rule_f3";
       } "rule_f4";
       src -> patient.active = evaluate(item, ${"$"}this.item.where(linkId = 'PR-active').answer.value) "rule_h";
       item.item as addressBlock where (linkId = 'PR-address') -> patient.address = create('Address') as patientAddress then {
          src -> patientAddress.city = evaluate(addressBlock, ${"$"}this.item.where(linkId = 'PR-address-city').answer.value) "rule_g1";
          src -> patientAddress.country = evaluate(addressBlock, ${"$"}this.item.where(linkId = 'PR-address-country').answer.value) "rule_g2";
          src -> patientAddress.use = "home" "rule_g3";
          src -> patientAddress.type = "physical" "rule_g4";
       } "rule_g";
    };
}

group ExtractRelatedPerson(source src : QuestionnaireResponse, target bundle : Bundle, source patient : Patient) {
    src -> bundle.entry as entry, entry.resource = create('RelatedPerson') as relatedPerson then {
        src.item as item where(linkId = 'RP') then {
            src -> relatedPerson.name = create('HumanName') as relatedPersonName then {
                src -> relatedPersonName.family = evaluate(item, ${"$"}this.item.where(linkId = 'RP-family-name').answer.value) "rule_erp_2";
                src -> relatedPersonName.given = evaluate(item, ${"$"}this.item.where(linkId = 'RP-first-name').answer.value) "rule_erp_3";
            } "rule_erp_1";
            item.item as patientRelationship where(linkId = 'RP-relationship' and answer.count() > 0 and answer[0].empty().not()) then {
                  src -> evaluate(patientRelationship, ${"$"}this.answer.value) as relationshipString then {
                      src -> relatedPerson.relationship = cc("http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype", relationshipString) "rule_erp_4a";
                  } "rule_erp_4";
            } "rule_erp_4__3";
            src -> relatedPerson.telecom = create('ContactPoint') as relatedPersonContact then {
                src -> relatedPersonContact.system = "phone" "rule_erp_5";
                src -> relatedPersonContact.value = evaluate(item, ${"$"}this.item.where(linkId = 'RP-contact-1').answer.value) "rule_erp_6";
                src -> relatedPersonContact.rank = create('positiveInt') as posInt then {
                    src -> posInt.value = "1" "rule_erp_7a";
                } "rule_erp_7";
            } "rule_erp_7b";

            src -> relatedPerson.telecom = create('ContactPoint') as relatedPersonContact then {
                src -> relatedPersonContact.system = "phone" "rule_erp_8";
                src -> relatedPersonContact.value = evaluate(item, ${"$"}this.item.where(linkId = 'RP-contact-alternate').answer.value) "rule_erp_9";
                src -> relatedPersonContact.rank = create('positiveInt') as posInt then {
                  src -> posInt.value = "2" "rule_erp_10b";
                } "rule_erp_10";
            } "rule_erp_10a";
          
            src -> relatedPerson.id = uuid() "rule_erp_11_a";
            src.item as relatedPersonIdItem where(linkId = 'RP-id' and answer.count() > 0 and answer.empty().not()) then {
                src -> relatedPerson.id = create('id') as patientId then { 
                  src -> patientId.value = evaluate(relatedPersonIdItem, ${"$"}this.answer.value) "rule_erp_11_1_1";
                } "rule_erp_11_1";
            } "rule_erp_11";
            
            patient -> relatedPerson.patient = reference(patient) "rule_erp_13a";
        };
    } "rule_erp_14";
}

group ExtractObservations(source src : QuestionnaireResponse, target bundle : Bundle, source patient : Patient) {
    src.item as item where(linkId = 'comorbidities') then {
    	item.answer as itemAns -> bundle.entry as entry, entry.resource = create('Observation') as obs then {
	    	src -> obs.id = uuid() "rule_eo1";
	    	src -> obs.effective = evaluate(patient, now()) "rule_eo2";
	    	src -> obs.subject = reference(patient) "rule_eo3";
	    	src -> obs.code = cc("https://www.snomed.org", "991381000000107") "rule_eo4";
	    	src -> obs.status = "final" "rule_eo5";
	    	itemAns.value as itemValue -> obs.value = create('CodeableConcept') as codeableConcept then {
              itemValue.display as itemValueText -> codeableConcept.text = itemValueText "rule_eo6_1";
              itemValue -> codeableConcept.coding = itemValue "rule_eo6_2";
              
              itemValue where(itemValue.code = '74964007') -> bundle.entry as entry, entry.resource = create('Observation') as obs2 then {
                src.item as otherComorbItem where(linkId = 'other_comorbidities' and answer.count() > 0 and answer[0].empty().not()) then {
                  src -> obs2.id = uuid() "rule_eo6_3_1_1";
                  src -> obs2.effective = evaluate(patient, now()) "rule_eo6_3_1_2";
                  src -> obs2.subject = reference(patient) "rule_eo6_3_1_3";
                  src -> obs2.code = cc("https://www.snomed.org", "38651000000103") "rule_eo6_3_1_4";
                  src -> obs2.status = "final" "rule_eo6_3_1_5";
                  src -> obs2.value = evaluate(otherComorbItem , ${"$"}this.answer[0].value) "rule_eo6_3_1_5";
                } "rule_eo6_3_1";
              } "rule_eo6_3";
            } "rule_eo6";
    	} "rule_e08";
	} "rule_eo7";
}


group ExtractRiskAssessmentObservation(source src : QuestionnaireResponse, source bundleEn : Bundle, target bundle : Bundle, source patient : Patient) {
    src.item as item where(linkId = 'comorbidities' and answer.count() > 0) -> bundle.entry as entry, entry.resource = create('RiskAssessment') as riskAm then {
        src -> riskAm.id = uuid() "rule_erao_1";
        src -> riskAm.code = create('CodeableConcept') as riskAmCc then {
          src -> riskAmCc.text = "Client is at risk for serious illness from COVID-19" "rule_erao_2_1";
          src -> riskAmCc.coding = c("https://www.snomed.org", "225338004", "Risk Assessment") "rule_erao_2_2";
        } "rule_erao_2";
        src -> riskAm.status = "final" "rule_erao_3";
        src -> riskAm.subject = reference(patient) "rule_erao_4";
        src -> riskAm.occurrence = evaluate(patient, now()) "rule_erao_5";
        src -> riskAm.prediction = create('RiskAssessment_Prediction') as riskPrediction then {
          src -> riskPrediction.relativeRisk = evaluate(bundle, ${"$"}this.entry.where(resource.is(Observation) and resource.code.coding[0].code = '991381000000107').count()) "rule_erao_6_1";
          src -> riskPrediction.outcome = cc("https://www.snomed.org", "38651000000103") "rule_erao_6_2";
        } "rule_erao_6";
        bundleEn.entry as resourceEntry where(resource.is(Observation) and resource.code.coding[0].code = '991381000000107') then {
          resourceEntry.resource as rs -> riskAm.basis = reference(rs) "rule_erao_7_1"; 
        } "rule_erao_7";
        
        src -> bundle.entry as entry, entry.resource = create('Flag') as riskFlag then {
          src -> riskFlag.id = uuid() "rule_erao_8_1";
          src -> riskFlag.status = "active" "rule_erao_8_2";
          src -> riskFlag.subject = reference(patient) "rule_erao_8_3";
          src -> riskFlag.code = create('CodeableConcept') as riskFlagCc then {
            src -> riskFlagCc.text = "Client is at risk for serious illness from COVID-19" "rule_erao_8_4_1";
            src -> riskFlagCc.coding = c("https://www.snomed.org", "225338004", "Risk Assessment") "rule_erao_8_4_2";
          } "rule_erao_8_4";
          src -> riskFlag.extension = create('Extension') as flagDetailExt then {
            src -> flagDetailExt.url = "http://hl7.org/fhir/StructureDefinition/flag-detail" "rule_erao_8_5_1";
            src -> flagDetailExt.value = reference(riskAm) "rule_erao_8_5_2";
          } "rule_erao_8_5";
        } "rule_erao_8";
    } "rule_erao_9";
}
    """.trimIndent()

  private val RELATED_PESRON_JSON =
    """
    {"resourceType":"RelatedPerson","id":"7b93dcf1-2ca1-4106-a661-a69ab235facf","meta":{"versionId":"1","lastUpdated":"2021-09-08T08:57:06.038+00:00","source":"#a745b979452223b7"},"patient":{"reference":"Patient/bf04d957-dd5c-4fd4-b1e3-31de3f21a9dc"},"relationship":[{"coding":[{"system":"http://terminology.hl7.org/CodeSystem/v3-RoleCode","code":"Father","display":"father"}]}],"name":[{"family":"Victor","given":["Sumaria"]}],"telecom":[{"system":"phone","rank":1},{"system":"phone","rank":2}]}
  """
  private val PATIENT_JSON =
    """
      {"resourceType":"Patient","id":"bf04d957-dd5c-4fd4-b1e3-31de3f21a9dc","meta":{"versionId":"1","lastUpdated":"2021-09-08T08:57:03.765+00:00","source":"#9b6a2cfb65269f5c"},"text":{"status":"generated","div":"<div xmlns=\"http://www.w3.org/1999/xhtml\"><div class=\"hapiHeaderText\">Jared <b>JOHN </b></div><table class=\"hapiPropertyTable\"><tbody><tr><td>Address</td><td><span>Nairobi </span><span>Kenya </span></td></tr><tr><td>Date of birth</td><td><span>17 August 2021</span></td></tr></tbody></table></div>"},"active":true,"name":[{"family":"John","given":["Jared"]}],"telecom":[{"system":"phone","value":"0702125232","rank":1}],"gender":"female","birthDate":"2021-08-17","address":[{"use":"home","type":"physical","city":"Nairobi","country":"Kenya"}]}
    """.trimIndent()
}
