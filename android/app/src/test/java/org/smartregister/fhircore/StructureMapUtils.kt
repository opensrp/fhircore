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
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager
import org.hl7.fhir.utilities.npm.ToolsVersion
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.fhircore.shadow.FhirApplicationShadow

@Config(shadows = [FhirApplicationShadow::class])
class StructureMapUtils : RobolectricTest() {

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

    val scu = org.hl7.fhir.r4.utils.StructureMapUtilities(contextR4)
    val map = scu.parse(fhirMapToConvert, "PatientRegistration")

    val iParser: IParser = FhirContext.forR4().newJsonParser()
    val mapString = iParser.encodeResourceToString(map)

    System.out.println(mapString)

    // Encode
    //val targetResource = ResourceFactory.createResource(scu.getTargetType(map).getName())
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
        "id": "client-registration-sample",
        "status": "active",
        "date": "2020-11-18T07:24:47.111Z",
        "subjectType": [
          "Patient"
        ],
        "extension": [
          {
            "url": "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-targetStructureMap",
            "valueCanonical": "https://fhir.labs.smartregister.org/StructureMap/383"
          }
        ],
        "item": [
          {
            "linkId": "PR",
            "type": "group",
            "text": "Client Info",
            "_text": {
              "extension": [
                {
                  "extension": [
                    {
                      "url": "lang",
                      "valueCode": "sw"
                    },
                    {
                      "url": "content",
                      "valueString": "Maelezo ya mteja"
                    }
                  ],
                  "url": "http://hl7.org/fhir/StructureDefinition/translation"
                }
              ]
            },
            "item": [
              {
                "linkId": "PR-name",
                "type": "group",
                "item": [
                  {
                    "linkId": "PR-name-given",
                    "type": "string",
                    "required": true,
                    "text": "First Name",
                    "_text": {
                      "extension": [
                        {
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Jina la kwanza"
                            }
                          ],
                          "url": "http://hl7.org/fhir/StructureDefinition/translation"
                        }
                      ]
                    }
                  },
                  {
                    "linkId": "PR-name-family",
                    "type": "string",
                    "required": true,
                    "text": "Family Name",
                    "_text": {
                      "extension": [
                        {
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Jina la ukoo"
                            }
                          ],
                          "url": "http://hl7.org/fhir/StructureDefinition/translation"
                        }
                      ]
                    }
                  }
                ]
              },
              {
                "linkId": "patient-0-birth-date",
                "type": "date",
                "required": true,
                "text": "Date of Birth",
                "_text": {
                  "extension": [
                    {
                      "extension": [
                        {
                          "url": "lang",
                          "valueCode": "sw"
                        },
                        {
                          "url": "content",
                          "valueString": "Tarehe ya kuzaliwa"
                        }
                      ],
                      "url": "http://hl7.org/fhir/StructureDefinition/translation"
                    }
                  ]
                }
              },
              {
                "linkId": "patient-0-gender",
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
                  }
                ],
                "type": "choice",
                "text": "Gender",
                "initial": [
                  {
                    "valueCoding": {
                      "code": "female",
                      "display": "Female"
                    }
                  }
                ],
                "answerOption": [
                  {
                    "valueCoding": {
                      "code": "female",
                      "display": "Female",
                      "designation": [
                        {
                          "language": "sw",
                          "value": "Mwanamke"
                        }
                      ]
                    }
                  },
                  {
                    "valueCoding": {
                      "code": "male",
                      "display": "Male",
                      "designation": [
                        {
                          "language": "sw",
                          "value": "Mwanaume"
                        }
                      ]
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
                    "type": "string",
                    "text": "system",
                    "initial": [
                      {
                        "valueString": "phone"
                      }
                    ],
                    "enableWhen": [
                      {
                        "question": "patient-0-gender",
                        "operator": "=",
                        "answerString": "ok"
                      }
                    ]
                  },
                  {
                    "linkId": "PR-telecom-value",
                    "type": "string",
                    "required": true,
                    "text": "Phone Number",
                    "_text": {
                      "extension": [
                        {
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Nambari ya simu"
                            }
                          ],
                          "url": "http://hl7.org/fhir/StructureDefinition/translation"
                        }
                      ]
                    }
                  }
                ]
              },
              {
                "linkId": "PR-address",
                "type": "group",
                "item": [
                  {
                    "linkId": "PR-address-city",
                    "type": "string",
                    "text": "City",
                    "_text": {
                      "extension": [
                        {
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Mji"
                            }
                          ],
                          "url": "http://hl7.org/fhir/StructureDefinition/translation"
                        }
                      ]
                    }
                  },
                  {
                    "linkId": "PR-address-country",
                    "type": "string",
                    "text": "Country",
                    "_text": {
                      "extension": [
                        {
                          "extension": [
                            {
                              "url": "lang",
                              "valueCode": "sw"
                            },
                            {
                              "url": "content",
                              "valueString": "Nchi"
                            }
                          ],
                          "url": "http://hl7.org/fhir/StructureDefinition/translation"
                        }
                      ]
                    }
                  }
                ]
              },
              {
                "linkId": "PR-active",
                "type": "boolean",
                "text": "Is Active?",
                "_text": {
                  "extension": [
                    {
                      "extension": [
                        {
                          "url": "lang",
                          "valueCode": "sw"
                        },
                        {
                          "url": "content",
                          "valueString": "Inatumika?"
                        }
                      ],
                      "url": "http://hl7.org/fhir/StructureDefinition/translation"
                    }
                  ]
                }
              }
            ]
          },
          {
            "linkId": "RP",
            "type": "group",
            "text": "Related person",
            "item": [
              {
                "linkId": "RP-family-name",
                "text": "Family name",
                "required": true,
                "type": "text"
              },
              {
                "linkId": "RP-first-name",
                "text": "First name",
                "required": true,
                "type": "text"
              },
              {
                "linkId": "RP-relationship",
                "text": "Relationship to patient",
                "required": true,
                "type": "text",
                "answerValueSet": "http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype"
              },
              {
                "linkId": "RP-contact-1",
                "text": "Phone number",
                "required": true,
                "type": "text"
              },
              {
                "linkId": "RP-contact-alternate",
                "text": "Alternative phone number",
                "type": "text"
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
uses "http://hl7.org/fhir/StructureDefinition/Patient" as source

group PatientRegistration(source src : QuestionnaireResponse, target bundle: Bundle) {
    src -> bundle.id = uuid() "rule_c";
    src -> bundle.type = 'collection' "rule_b";
    src -> bundle.entry as entry, entry.resource = create('Patient') as patient then
        ExtractPatient(src, patient), ExtractRelatedPerson(src, bundle, patient) "rule_i";
}

group ExtractPatient(source src : QuestionnaireResponse, target patient : Patient) {
    src -> patient.id = uuid() "rule_j";

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



group ExtractRelatedPerson(source src : QuestionnaireResponse, target bundle : Bundle, source patientId : Patient) {
    src -> bundle.entry as entry, entry.resource = create('RelatedPerson') as relatedPerson then {
        src.item as item where(linkId = 'RP') then {
            src -> relatedPerson.name = create('HumanName') as relatedPersonName then {
                src -> relatedPersonName.family = evaluate(item, ${"$"}this.item.where(linkId = 'RP-family-name').answer.value) "rule_erp_2";
                src -> relatedPersonName.given = evaluate(item, ${"$"}this.item.where(linkId = 'RP-first-name').answer.value) "rule_erp_3";
            } "rule_erp_1";
            src -> evaluate(item, ${"$"}this.item.where(linkId = 'RP-relationship').answer.value) as relationshipString then {
                src -> relatedPerson.relationship = cc("http://hl7.org/fhir/ValueSet/relatedperson-relationshiptype", relationshipString) "rule_erp_4a";
            } "rule_erp_4";
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

            src -> relatedPerson.id = uuid() "rule_erp_11";
            patientId -> relatedPerson.patient = create('Reference') as patientReference then {
                patientId.id as thePatientId  -> patientReference.reference = thePatientId "rule_erp_12";
                src -> patientReference.type = "Patient" "rule_erp_13";
            } "rule_erp_13a";
        };
    } "rule_erp_14";
}
""".trimIndent()
}
