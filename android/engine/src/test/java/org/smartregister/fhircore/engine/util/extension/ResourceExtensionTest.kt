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

package org.smartregister.fhircore.engine.util.extension

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.datacapture.extensions.logicalId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Timing
import org.hl7.fhir.r4.model.UriType
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.LinkIdConfig
import org.smartregister.fhircore.engine.configuration.LinkIdType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor

@HiltAndroidTest
class ResourceExtensionTest : RobolectricTest() {

  @get:Rule(order = 0) val hiltAndroidRule = HiltAndroidRule(this)

  @Inject lateinit var fhirPathDataExtractor: FhirPathDataExtractor

  private val context = ApplicationProvider.getApplicationContext<Application>()

  @Before
  fun setUp() {
    hiltAndroidRule.inject()
  }

  @Test
  fun `JSONObject#updateFrom() should ignore nulls and update fields`() {
    val originalObject = JSONObject()
    originalObject.put("property1", "ValueProperty1")
    originalObject.put("property2", "ValueProperty2")
    originalObject.put("prop3", "Medical")
    originalObject.put("prop4", "VP4")
    originalObject.putOpt("prop5", null)

    val updatedObject = JSONObject()
    updatedObject.put("prop3", "ValueProp3")
    updatedObject.put("prop4", null as Any?)
    updatedObject.put("prop5", "ValueProp5")

    originalObject.updateFrom(updatedObject)

    Assert.assertEquals("ValueProp3", originalObject.get("prop3"))
    Assert.assertEquals("VP4", originalObject.get("prop4"))
    Assert.assertEquals("ValueProp5", originalObject.get("prop5"))
  }

  @Test
  fun `Resource#updateFrom() should ignore nulls`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
  }

  @Test
  fun `Resource#updateFrom() should preserve previous patient's meta tags`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals("hello", patient.meta.tag[0].display)
    Assert.assertEquals("hello2", patient.meta.tag[1].display)
  }

  @Test
  fun `Resource#updateFrom() should preserve previous patient's extensions`() {
    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList = arrayListOf<Extension>()

    extensionList.add(extensionVal)
    extensionList.add(extensionVal2)

    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        extension.apply { addAll(extensionList) }
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve updated patient's extensions`() {
    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList = arrayListOf<Extension>()

    extensionList.add(extensionVal)
    extensionList.add(extensionVal2)

    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null

        extension.apply { addAll(extensionList) }
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve both resource's extensions`() {
    val extensionVal = Extension()
    val extensionVal2 = Extension()
    extensionVal.apply { this.urlElement = UriType("hello") }
    extensionVal2.apply { this.urlElement = UriType("hello2") }

    val extensionList1 = arrayListOf<Extension>()
    val extensionList2 = arrayListOf<Extension>()

    extensionList1.add(extensionVal)
    extensionList2.add(extensionVal2)
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        extension.apply { addAll(extensionList1) }
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = null
        extension.apply { addAll(extensionList2) }
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals(2, patient.extension.size)
  }

  @Test
  fun `Resource#updateFrom() should preserve updated resource's meta tags`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals("hello", patient.meta.tag[0].display)
    Assert.assertEquals("hello2", patient.meta.tag[1].display)
  }

  @Test
  fun `Resource#updateFrom() should preserve meta tags of both resources`() {
    var patient =
      Patient().apply {
        active = true
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc3", "xyz3", "hello3"))
          addAll(codingList)
        }
        name.apply {
          add(
            HumanName().apply {
              family = "Doe"
              given = listOf(StringType("John"))
            },
          )
        }
      }

    val updatedPatient =
      Patient().apply {
        deceased = BooleanType(true)
        birthDate = Date()
        gender = Enumerations.AdministrativeGender.FEMALE
        meta.tag.apply {
          val codingList = arrayListOf<Coding>()
          codingList.add(Coding("abc", "xyz", "hello"))
          codingList.add(Coding("abc2", "xyz2", "hello2"))
          addAll(codingList)
        }
        name.apply {
          add(
            HumanName().apply {
              family = "Kamau"
              given = listOf(StringType("Andrew"))
            },
          )
        }
      }

    patient = patient.updateFrom(updatedPatient)

    Assert.assertNotNull(patient.birthDate)
    Assert.assertEquals(
      BooleanType(true).booleanValue(),
      (patient.deceased as BooleanType).booleanValue(),
    )
    Assert.assertEquals("Kamau", patient.name[0].family)
    Assert.assertEquals("Andrew", patient.name[0].given[0].value)
    Assert.assertEquals(3, patient.meta.tag.size)
  }

  @Test
  fun `QuestionnaireResponse#deleteRelatedResources() should call defaultRepository#deleteResource for resources in contained`() {
    val patient = Patient()
    val relatedPerson = RelatedPerson()
    val observation = Observation()
    val questionnaireResponse =
      QuestionnaireResponse().apply { contained = listOf(patient, relatedPerson, observation) }

    val defaultRepository = mockk<DefaultRepository>()

    coEvery { defaultRepository.delete(any()) } returns Unit

    runBlocking { questionnaireResponse.deleteRelatedResources(defaultRepository) }

    coVerify { defaultRepository.delete(patient) }
    coVerify { defaultRepository.delete(relatedPerson) }
    coVerify { defaultRepository.delete(observation) }
  }

  @Test
  fun `QuestionnaireResponse#retainMetadata() should call retain details from previous QR`() {
    val id = "qrId"
    val authoredDate = Date()
    val versionId = "5"
    val author = Reference()
    val oldQr =
      QuestionnaireResponse().apply {
        setId(id)
        authored = authoredDate
        setAuthor(author)
        meta.apply { setVersionId(versionId) }
      }
    val questionnaireResponse = QuestionnaireResponse()

    questionnaireResponse.retainMetadata(oldQr)

    Assert.assertEquals(id, oldQr.id)
    Assert.assertEquals(author, oldQr.author)
    Assert.assertEquals(authoredDate, oldQr.authored)
    Assert.assertEquals("6", oldQr.meta.versionId)
    Assert.assertNotNull(oldQr.meta.lastUpdated)
  }

  @Test
  fun `QuestionnaireResponse#getEncounterId() should return logicalId`() {
    val questionnaireResponse =
      QuestionnaireResponse().apply { contained = listOf(Encounter().apply { id = "1234" }) }

    val id = questionnaireResponse.getEncounterId()

    Assert.assertEquals("1234", id)
  }

  @Test
  fun `QuestionnaireResponse#getEncounterId() replace# should return logicalId`() {
    val questionnaireResponse =
      QuestionnaireResponse().apply { contained = listOf(Encounter().apply { id = "#1234" }) }

    val id = questionnaireResponse.getEncounterId()

    Assert.assertEquals("1234", id)
  }

  @Test
  fun `QuestionnaireResponse#getEncounterId() Id null should return empty id`() {
    val questionnaireResponse = QuestionnaireResponse().apply { contained = listOf(Encounter()) }

    val id = questionnaireResponse.getEncounterId()

    Assert.assertEquals("", id)
  }

  @Test
  fun `Resource#generateMissingId() should generate Id if empty`() {
    val resource = Patient()
    resource.id = "1"

    resource.generateMissingId()
    Assert.assertEquals("1", resource.logicalId)

    resource.id = null
    resource.generateMissingId()
    Assert.assertNotEquals("1", resource.logicalId)
    Assert.assertFalse(resource.logicalId.isEmpty())
  }

  @Test
  fun `Type#valueToString() should return string representation`() {
    Assert.assertEquals("", null.valueToString())
    Assert.assertEquals("12345", StringType("12345").valueToString())
    Assert.assertEquals("true", BooleanType(true).valueToString())
    Assert.assertEquals(Date().makeItReadable(), DateTimeType(Date()).valueToString())
    Assert.assertEquals("d", Coding("s", "c", "d").valueToString())
    Assert.assertEquals("c", Coding().apply { code = "c" }.valueToString())
    Assert.assertEquals(
      "d",
      CodeableConcept().apply { addCoding(Coding("s", "c", "d")) }.valueToString(),
    )
    Assert.assertEquals(
      "3.4",
      Quantity()
        .apply {
          this.value = BigDecimal.valueOf(3.4)
          this.unit = "G"
        }
        .valueToString(),
    )
    Assert.assertEquals(
      "8 Week (s)",
      Timing()
        .apply {
          repeat.period = BigDecimal(8.0)
          repeat.periodUnit = Timing.UnitsOfTime.WK
        }
        .valueToString(),
    )
    Assert.assertEquals("Doe", HumanName().apply { family = "Doe" }.valueToString())
    Assert.assertEquals(
      "John Doe",
      HumanName()
        .apply {
          given = listOf(StringType("John"))
          family = "Doe"
        }
        .valueToString(),
    )
    Assert.assertEquals("John", StringType("John").valueToString())
  }

  @Test
  fun `Resource#generateReferenceValue() should return correct reference`() {
    val resource = Patient().apply { id = "123456" }

    Assert.assertEquals("Patient/123456", resource.referenceValue())
  }

  @Test
  fun `Type valueToString() should return string representation`() {
    Assert.assertEquals("12345", StringType("12345").valueToString())
    Assert.assertEquals("true", BooleanType(true).valueToString())
    Assert.assertEquals(Date().makeItReadable(), DateTimeType(Date()).valueToString())
    Assert.assertEquals("d", Coding("s", "c", "d").valueToString())
    Assert.assertEquals(
      "d",
      CodeableConcept().apply { addCoding(Coding("s", "c", "d")) }.valueToString(),
    )
    Assert.assertEquals(
      "3.4",
      Quantity()
        .apply {
          this.value = BigDecimal.valueOf(3.4)
          this.unit = "G"
        }
        .valueToString(),
    )
  }

  @Test
  fun `Resource generateReferenceValue() should return correct reference`() {
    val resource = Patient().apply { id = "123456" }

    Assert.assertEquals("Patient/123456", resource.referenceValue())
  }

  @Test
  fun `Patient referenceParamForCondition() should return correct reference param`() {
    val result = Patient().referenceParamForCondition()

    Assert.assertEquals(Condition.PATIENT, result)
  }

  @Test
  fun `Encounter referenceParamForCondition() should return correct reference param`() {
    val result = Encounter().referenceParamForCondition()

    Assert.assertEquals(Condition.ENCOUNTER, result)
  }

  @Test
  fun `Patient referenceParamForObservation() should return correct reference param`() {
    val result = Patient().referenceParamForObservation()

    Assert.assertEquals(Observation.PATIENT, result)
  }

  @Test
  fun `Encounter referenceParamForObservation() should return correct reference param`() {
    val result = Encounter().referenceParamForObservation()

    Assert.assertEquals(Observation.ENCOUNTER, result)
  }

  @Test
  fun `QuestionnaireResponse referenceParamForObservation() should return correct reference param`() {
    val result = QuestionnaireResponse().referenceParamForObservation()

    Assert.assertEquals(Observation.FOCUS, result)
  }

  @Test
  fun `isValidResourceType() should return true if resource type is valid`() {
    Assert.assertTrue(isValidResourceType("Patient"))
    Assert.assertTrue(isValidResourceType("Group"))
  }

  @Test
  fun `isValidResourceType() should return false if resource type is not valid`() {
    Assert.assertFalse(isValidResourceType("Client"))
    Assert.assertFalse(isValidResourceType("Manufacturer"))
    Assert.assertFalse(isValidResourceType(""))
  }

  @Test
  fun logicalIdFromFhirPathExtractedIdReturnsCorrectValue() {
    val logicalId = "Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4/_history/2"
    Assert.assertEquals("0acda8c9-3fa3-40ae-abcd-7d1fba7098b4", logicalId.extractLogicalIdUuid())
    val otherLogicalId = "Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4"
    Assert.assertEquals(
      "0acda8c9-3fa3-40ae-abcd-7d1fba7098b4",
      otherLogicalId.extractLogicalIdUuid(),
    )
  }

  @Test
  fun `prepareQuestionsForReadingOrEditing should set readOnly to true when passed`() {
    val questionnaire = Questionnaire()
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "1" })
    questionnaire.item.add(
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "2"
        type = Questionnaire.QuestionnaireItemType.GROUP
      },
    )
    questionnaire.item.prepareQuestionsForReadingOrEditing("", true)

    Assert.assertTrue(questionnaire.item[0].readOnly)
    Assert.assertFalse(questionnaire.item[1].readOnly)
  }

  @Test
  fun `prepareQuestionsForReadingOrEditing should set readOnly correctly when true not passed`() {
    val questionnaire = Questionnaire()
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "1" })
    questionnaire.item.add(
      Questionnaire.QuestionnaireItemComponent().apply {
        linkId = "2"
        readOnly = true
      },
    )
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "3" })
    questionnaire.item.prepareQuestionsForReadingOrEditing("", readOnlyLinkIds = listOf("3"))

    Assert.assertFalse(questionnaire.item[0].readOnly)
    Assert.assertTrue(questionnaire.item[1].readOnly)
    Assert.assertTrue(questionnaire.item[2].readOnly)
  }

  @Test
  fun testFilterByExpression() {
    val tasks =
      listOf(
        RepositoryResourceData(
          resource =
            Task().apply {
              id = "Task/task1"
              description = "New Task"
              status = Task.TaskStatus.READY
              executionPeriod =
                Period().apply {
                  start = Date().plusMonths(-1)
                  end = Date().plusDays(-1)
                }
              addBasedOn(Reference("care1"))
            },
        ),
        RepositoryResourceData(
          resource =
            Task().apply {
              id = "Task/task2"
              description = "Another task"
              status = Task.TaskStatus.READY
              executionPeriod =
                Period().apply {
                  start = Date().plusMonths(-1)
                  end = Date().plusDays(-1)
                }
              addBasedOn(Reference("CarePlan/care2"))
            },
        ),
      )

    // Task with malformed basedOn references
    val filteredTasks =
      tasks.filterByFhirPathExpression(
        fhirPathDataExtractor = fhirPathDataExtractor,
        conditionalFhirPathExpressions =
          listOf("Task.basedOn[0].reference.startsWith('CarePlan').not()"),
        matchAll = true,
      )

    Assert.assertTrue(filteredTasks.isNotEmpty())
    Assert.assertEquals(filteredTasks.first().resource.logicalId, tasks.first().resource.logicalId)

    // Task with correct basedOn references
    val filteredTasks2 =
      tasks.filterByFhirPathExpression(
        fhirPathDataExtractor = fhirPathDataExtractor,
        conditionalFhirPathExpressions = listOf("Task.basedOn[0].reference.startsWith('CarePlan')"),
        matchAll = true,
      )

    Assert.assertTrue(filteredTasks2.isNotEmpty())
    Assert.assertEquals(filteredTasks2.first().resource.logicalId, tasks.last().resource.logicalId)
  }

  @Test
  fun testAppendRelatedEntityLocationUpdatesResourceMetadataTag() {
    val group =
      Group().apply {
        id = "grp1"
        meta.addTag(Coding("http://system254.url", "123", "Sample code"))
      }
    val theLinkId = "someLinkId"
    val locationId = "awesome-location-uuid"
    val questionnaireResponse =
      QuestionnaireResponse().apply {
        id = "qr1"
        addItem(
          QuestionnaireResponse.QuestionnaireResponseItemComponent(StringType(theLinkId)).apply {
            addAnswer(
              QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
                setValue(StringType(locationId))
              },
            )
          },
        )
      }
    val questionnaireConfig =
      QuestionnaireConfig(
        id = "someQuestionnaire",
        linkIds =
          listOf(
            LinkIdConfig(linkId = theLinkId, LinkIdType.LOCATION),
          ),
      )

    group.appendRelatedEntityLocation(questionnaireResponse, questionnaireConfig, context)

    Assert.assertEquals(2, group.meta.tag.size)

    val firstMetaTag = group.meta.tag.firstOrNull()
    Assert.assertNotNull(firstMetaTag)
    Assert.assertEquals("http://system254.url", firstMetaTag?.system)
    Assert.assertEquals("123", firstMetaTag?.code)

    val lastMetaTag = group.meta.tag.lastOrNull()
    Assert.assertNotNull(lastMetaTag)
    Assert.assertEquals(
      context.getString(
        org.smartregister.fhircore.engine.R.string.sync_strategy_related_entity_location_system,
      ),
      lastMetaTag?.system,
    )
    Assert.assertEquals(locationId, lastMetaTag?.code)
  }

  @Test
  fun `test Organization Info Appended on Encounter Resource`() {
    val encounter = Encounter().apply { this.id = "123456" }
    encounter.appendOrganizationInfo(listOf("Organization/12345"))
    Assert.assertEquals("Organization/12345", encounter.serviceProvider.reference)
  }

  @Test
  fun `test Organization Info Appended on Location Resource`() {
    val location = Location().apply { this.id = "123456" }
    location.appendOrganizationInfo(listOf("Organization/12345"))
    Assert.assertEquals("Organization/12345", location.managingOrganization.reference)
  }

  @Test
  fun `test Organization Info Appended on Group Resource`() {
    val group = Group().apply { this.id = "123456" }
    group.appendOrganizationInfo(listOf("Organization/12345"))
    Assert.assertEquals("Organization/12345", group.managingEntity.reference)
  }

  @Test
  fun `test Organization Info Appended on Patient Resource`() {
    val patient = Patient().apply { this.id = "123456" }
    patient.appendOrganizationInfo(listOf("Organization/12345"))
    Assert.assertEquals("Organization/12345", patient.managingOrganization.reference)
  }

  @Test
  fun `test Organization Info Appended on Consent Resource`() {
    val consent = Consent().apply { this.id = "123456" }
    consent.appendOrganizationInfo(listOf("Organization/12345"))
    Assert.assertEquals("Organization/12345", consent.organization.first().reference)
  }

  @Test
  fun `prepareQuestionsForEditing should set readOnly correctly when readOnlyLinkIds passed`() {
    val questionnaire = Questionnaire()
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "1" })
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "2" })
    questionnaire.item.add(Questionnaire.QuestionnaireItemComponent().apply { linkId = "3" })
    questionnaire.item.prepareQuestionsForEditing("", readOnlyLinkIds = listOf("1", "3"))

    Assert.assertTrue(questionnaire.item[0].readOnly)
    Assert.assertFalse(questionnaire.item[1].readOnly)
    Assert.assertTrue(questionnaire.item[2].readOnly)
  }

  @Test
  fun testExtractGenderShouldReturnMaleStringWhenPatientGenderIsMale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.MALE }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.male),
      patient.extractGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testExtractGenderShouldReturnMaleStringWhenRelatedPersonGenderIsMale() {
    val relatedPerson = RelatedPerson().apply { gender = Enumerations.AdministrativeGender.MALE }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.male),
      relatedPerson.extractGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testExtractGenderShouldReturnFemaleStringWhenPatientGenderIsFemale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.FEMALE }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.female),
      patient.extractGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testExtractGenderShouldReturnOtherStringWhenPatientGenderIsOther() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.OTHER }

    val applicationContext = (ApplicationProvider.getApplicationContext() as Application)

    Assert.assertEquals(
      applicationContext.getString(R.string.other),
      patient.extractGender(applicationContext),
    )
  }

  @Test
  fun testExtractGenderShouldReturnUnknownStringWhenPatientGenderIsUnknown() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.UNKNOWN }

    Assert.assertEquals(
      (ApplicationProvider.getApplicationContext() as Application).getString(R.string.unknown),
      patient.extractGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testExtractGenderShouldReturnNullWhenPatientGenderIsNull() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.NULL }

    Assert.assertEquals("", patient.extractGender(ApplicationProvider.getApplicationContext()))
  }

  @Test
  fun testExtractGenderShouldReturnNullWhenResourceDoesNotHaveGenderField() {
    val resource = Task()

    Assert.assertEquals("", resource.extractGender(ApplicationProvider.getApplicationContext()))
  }

  @Test
  fun testTranslateMaleGender() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.MALE }
    Assert.assertEquals(
      "Male",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testTranslateFemaleGender() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.FEMALE }
    Assert.assertEquals(
      "Female",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext()),
    )
  }

  @Test
  fun testTranslateGenderReturnsUnknownWhenValeIsNotMaleOrFemale() {
    val patient = Patient().apply { gender = Enumerations.AdministrativeGender.OTHER }
    Assert.assertEquals(
      "Unknown",
      patient.gender.translateGender(ApplicationProvider.getApplicationContext()),
    )
  }

  private fun getDateFromDaysAgo(daysAgo: Long, localDateNow: LocalDate = LocalDate.now()): Date {
    val localDate = localDateNow.minusDays(daysAgo)
    return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
  }

  @Test
  fun testGetAgeString() {
    val expectedAge = "1y"
    Assert.assertEquals(
      expectedAge,
      calculateAge(
        getDateFromDaysAgo(365, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge2 = "1y 1m"
    // passing days value for 1y 1m
    Assert.assertEquals(
      expectedAge2,
      calculateAge(
        getDateFromDaysAgo(399, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge3 = "1y"
    // passing days value for 1y 1w
    Assert.assertEquals(
      expectedAge3,
      calculateAge(
        getDateFromDaysAgo(372, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge4 = "1m"
    Assert.assertEquals(
      expectedAge4,
      calculateAge(
        getDateFromDaysAgo(32, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge6 = "1w"
    Assert.assertEquals(
      expectedAge6,
      calculateAge(
        getDateFromDaysAgo(7, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge7 = "1w 2d"
    Assert.assertEquals(
      expectedAge7,
      calculateAge(
        getDateFromDaysAgo(9, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge8 = "3d"
    Assert.assertEquals(
      expectedAge8,
      calculateAge(
        getDateFromDaysAgo(3, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge9 = "1y 2m"
    Assert.assertEquals(
      expectedAge9,
      calculateAge(
        getDateFromDaysAgo(450, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge10 = "40y 3m"
    Assert.assertNotEquals(
      expectedAge10,
      calculateAge(
        getDateFromDaysAgo(14700, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge11 = "40y"
    Assert.assertEquals(
      expectedAge11,
      calculateAge(
        getDateFromDaysAgo(14700, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge12 = "0d"
    // if difference b/w current date and DOB is O from extractAge extension
    Assert.assertEquals(
      expectedAge12,
      calculateAge(
        getDateFromDaysAgo(0, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge13 = "1y 6m"
    // passing days value for 1y 6m
    Assert.assertEquals(
      expectedAge13,
      calculateAge(
        getDateFromDaysAgo(550, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )

    val expectedAge14 = "5y"
    // passing days value for 5y
    Assert.assertEquals(
      expectedAge14,
      calculateAge(
        getDateFromDaysAgo(1826, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )
  }

  @Test
  fun testGetAgeStringFor49DayPeriod() {
    val expectedAge5 = "1m 3w"
    Assert.assertEquals(
      expectedAge5,
      calculateAge(
        getDateFromDaysAgo(49, LocalDate.of(2023, 4, 4)),
        context,
        LocalDate.of(2023, 4, 4),
      ),
    )
  }

  @Test
  fun testExtractAgeReturnsCorrectDateStringForAPatient() {
    val patient =
      Patient().apply { birthDate = Calendar.getInstance().apply { add(Calendar.YEAR, -19) }.time }

    Assert.assertEquals("19y", patient.extractAge(context))
  }

  @Test
  fun testExtractAgeReturnsCorrectDateStringForARelatedPerson() {
    val relatedPerson =
      RelatedPerson().apply {
        birthDate = Calendar.getInstance().apply { add(Calendar.YEAR, -21) }.time
      }

    Assert.assertEquals("21y", relatedPerson.extractAge(context))
  }

  @Test
  fun testExtractAgeShouldReturnAnEmptyStringWhenResourceDoesNotHaveBirthDate() {
    val resource = Task()

    Assert.assertEquals("", resource.extractAge(context))
  }

  @Test
  fun testExtractAgeShouldReturnAgeStringFromDaysWhenPatientHasBirthDate() {
    val currentDate = LocalDate.now()
    val oneYearAgo = currentDate.minusYears(1)

    val calendar =
      Calendar.getInstance().apply {
        timeInMillis = oneYearAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
      }

    val patient = Patient().apply { birthDate = calendar.time }
    Assert.assertEquals("1y", patient.extractAge(context))
  }

  @Test
  fun extractBirthDateReturnsCorrectDateWhenResourceIsPatientAndHasAValidBirthDate() {
    val resource = Patient().setBirthDate(org.joda.time.LocalDate.parse("2015-10-03").toDate())

    Assert.assertEquals(
      "03/10/2015",
      resource.extractBirthDate()?.let { SimpleDateFormat("dd/MM/yyyy").format(it) },
    )
  }

  @Test
  fun extractBirthDateReturnsCorrectDateWhenResourceIsRelatedPersonAndHasAValidBirthDate() {
    val resource =
      RelatedPerson().setBirthDate(org.joda.time.LocalDate.parse("2015-10-03").toDate())

    Assert.assertEquals(
      "03/10/2015",
      resource.extractBirthDate()?.let { SimpleDateFormat("dd/MM/yyyy").format(it) },
    )
  }

  @Test
  fun extractBirthDateReturnsNullWhenResourceDoesNotHaveABirthDateField() {
    val resource = Task()

    Assert.assertNull(resource.extractBirthDate())
  }
}
