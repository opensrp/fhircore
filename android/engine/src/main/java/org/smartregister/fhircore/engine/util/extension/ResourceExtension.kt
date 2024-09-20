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

import android.content.Context
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.gclient.ReferenceClientParam
import com.google.android.fhir.datacapture.extensions.createQuestionnaireResponseItem
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.get
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Composition
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Flag
import org.hl7.fhir.r4.model.Group
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.ImplementationGuide
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Practitioner
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.RelatedPerson
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.model.Task
import org.hl7.fhir.r4.model.Timing
import org.hl7.fhir.r4.model.Type
import org.joda.time.Instant
import org.json.JSONException
import org.json.JSONObject
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.LinkIdType
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

const val REFERENCE = "reference"
const val PARTOF = "part-of"
private val fhirR4JsonParser = FhirContext.forR4Cached().getCustomJsonParser()

fun Base?.valueToString(datePattern: String = "dd-MMM-yyyy"): String {
  return when {
    this == null -> return ""
    this.isDateTime -> (this as BaseDateTimeType).value.makeItReadable(datePattern)
    this.isPrimitive -> (this as PrimitiveType<*>).asStringValue()
    this is Coding -> display ?: code
    this is CodeableConcept -> this.stringValue()
    this is Quantity -> this.value.toPlainString()
    this is Timing ->
      this.repeat.let {
        it.period
          .toPlainString()
          .plus(" ")
          .plus(
            it.periodUnit.display.replaceFirstChar { char ->
              if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            },
          )
          .plus(" (s)")
      }
    this is HumanName ->
      this.given.firstOrNull().let {
        (if (it != null) "${it.valueToString()} " else "").plus(this.family)
      }
    this is Patient ->
      this.nameFirstRep.nameAsSingleString +
        ", " +
        this.gender.name.first() +
        ", " +
        this.birthDate.yearsPassed()
    this is Practitioner -> this.nameFirstRep.nameAsSingleString
    this is Group -> this.name
    else -> this.toString()
  }
}

fun CodeableConcept.stringValue(): String =
  this.text ?: this.codingFirstRep.display ?: this.codingFirstRep.code

fun Resource.encodeResourceToString(parser: IParser = fhirR4JsonParser): String =
  parser.encodeResourceToString(this.copy())

fun StructureMap.encodeResourceToString(parser: IParser = fhirR4JsonParser): String =
  parser
    .encodeResourceToString(this)
    .replace("'months'", "\\\\'months\\\\'")
    .replace("'days'", "\\\\'days\\\\'")
    .replace("'years'", "\\\\'years\\\\'")
    .replace("'weeks'", "\\\\'weeks\\\\'")

fun <T> String.decodeResourceFromString(parser: IParser = fhirR4JsonParser): T =
  parser.parseResource(this) as T

fun <T : Resource> T.updateFrom(updatedResource: Resource): T {
  var extensionUpdateFrom = listOf<Extension>()
  if (updatedResource is Patient) {
    extensionUpdateFrom = updatedResource.extension
  }
  var extension = listOf<Extension>()
  if (this is Patient) {
    extension = this.extension
  }
  val jsonParser = fhirR4JsonParser
  val stringJson = encodeResourceToString(jsonParser)
  val originalResourceJson = JSONObject(stringJson)

  originalResourceJson.updateFrom(JSONObject(updatedResource.encodeResourceToString(jsonParser)))
  return jsonParser.parseResource(this::class.java, originalResourceJson.toString()).apply {
    val meta = this.meta
    val metaUpdateFrom = this@updateFrom.meta
    if ((meta == null || meta.isEmpty)) {
      if (metaUpdateFrom != null) {
        this.meta = metaUpdateFrom
        this.meta.tag = metaUpdateFrom.tag
      }
    } else {
      val setOfTags = mutableSetOf<Coding>()
      setOfTags.addAll(meta.tag)
      setOfTags.addAll(metaUpdateFrom.tag)
      this.meta.tag = setOfTags.distinctBy { it.code + it.system }
    }
    if (this is Patient && this@updateFrom is Patient && updatedResource is Patient) {
      if (extension.isEmpty()) {
        if (extensionUpdateFrom.isNotEmpty()) {
          this.extension = extensionUpdateFrom
        }
      } else {
        val setOfExtension = mutableSetOf<Extension>()
        setOfExtension.addAll(extension)
        setOfExtension.addAll(extensionUpdateFrom)
        this.extension = setOfExtension.distinct()
      }
    }
  }
}

@Throws(JSONException::class)
fun JSONObject.updateFrom(updated: JSONObject) {
  val keys =
    mutableListOf<String>().apply {
      keys().forEach { add(it) }
      updated.keys().forEach { add(it) }
    }

  keys.forEach { key -> updated.opt(key)?.run { put(key, this) } }
}

fun QuestionnaireResponse.generateMissingItems(questionnaire: Questionnaire) =
  questionnaire.item.generateMissingItems(this.item)

fun List<Questionnaire.QuestionnaireItemComponent>.generateMissingItems(
  qrItems: MutableList<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
) {
  this.forEachIndexed { index, qItem ->
    // generate complete hierarchy if response item missing otherwise check for nested items
    if (qrItems.isEmpty() || (index < qrItems.size && qItem.linkId != qrItems[index].linkId)) {
      qrItems.add(index, qItem.createQuestionnaireResponseItem())
    } else if (index < qrItems.size) {
      qItem.item.generateMissingItems(qrItems[index].item)
    }
  }
}

/**
 * Set all questions that are not of type [Questionnaire.QuestionnaireItemType.GROUP] to readOnly if
 * [readOnly] is true. This also generates the correct FHIRPath population expression for each
 * question when mapped to the corresponding [QuestionnaireResponse]
 */
fun List<Questionnaire.QuestionnaireItemComponent>.prepareQuestionsForReadingOrEditing(
  path: String = "QuestionnaireResponse.item",
  readOnly: Boolean = false,
  readOnlyLinkIds: List<String>? = emptyList(),
) {
  forEach { item ->
    if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
      item.readOnly = readOnly || item.readOnly || readOnlyLinkIds?.contains(item.linkId) == true
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').answer.item",
        readOnly,
      )
    } else {
      item.item.prepareQuestionsForReadingOrEditing(
        "$path.where(linkId = '${item.linkId}').item",
        readOnly,
      )
    }
  }
}

/**
 * Set all questions that are not of type [Questionnaire.QuestionnaireItemType.GROUP] to readOnly if
 * [readOnlyLinkIds] item are there while editing the form. This also generates the correct FHIRPath
 * population expression for each question when mapped to the corresponding [QuestionnaireResponse]
 */
fun List<Questionnaire.QuestionnaireItemComponent>.prepareQuestionsForEditing(
  path: String = "QuestionnaireResponse.item",
  readOnlyLinkIds: List<String>? = emptyList(),
) {
  forEach { item ->
    if (item.type != Questionnaire.QuestionnaireItemType.GROUP) {
      item.readOnly = readOnlyLinkIds?.contains(item.linkId) == true
      item.item.prepareQuestionsForEditing(
        "$path.where(linkId = '${item.linkId}').answer.item",
        readOnlyLinkIds,
      )
    } else {
      item.item.prepareQuestionsForEditing(
        "$path.where(linkId = '${item.linkId}').item",
        readOnlyLinkIds,
      )
    }
  }
}

/** Delete resources in [QuestionnaireResponse.contained] from the database */
suspend fun QuestionnaireResponse.deleteRelatedResources(defaultRepository: DefaultRepository) {
  contained.forEach { defaultRepository.delete(it) }
}

fun QuestionnaireResponse.retainMetadata(questionnaireResponse: QuestionnaireResponse) {
  author = questionnaireResponse.author
  authored = questionnaireResponse.authored
  id = questionnaireResponse.logicalId

  val versionId = Integer.parseInt(questionnaireResponse.meta.versionId ?: "1") + 1

  questionnaireResponse.meta.apply {
    lastUpdated = Date()
    setVersionId(versionId.toString())
  }
}

fun QuestionnaireResponse.getEncounterId(): String? {
  return this.contained
    ?.find { it.resourceType == ResourceType.Encounter }
    ?.logicalId
    ?.replace(
      "#",
      "",
    )
}

fun Resource.generateMissingId() {
  if (logicalId.isEmpty() || logicalId.isBlank()) id = UUID.randomUUID().toString()
}

fun Resource.appendOrganizationInfo(authenticatedOrganizationIds: List<String>?) {
  // Organization reference in shared pref as "Organization/some-gibberish-uuid"
  // Only set organization only if the desired Resource property is null
  authenticatedOrganizationIds?.let { ids ->
    val organizationRef =
      ids.firstOrNull()?.extractLogicalIdUuid()?.asReference(ResourceType.Organization)

    if (organizationRef != null) {
      when (this) {
        is Patient -> managingOrganization = updateReference(managingOrganization, organizationRef)
        is Group -> managingEntity = updateReference(managingEntity, organizationRef)
        is Encounter -> serviceProvider = updateReference(serviceProvider, organizationRef)
        is Location -> managingOrganization = updateReference(managingOrganization, organizationRef)
        is Consent -> organization = updateReferenceList(organization, organizationRef)
        else -> {}
      }
    }
  }
}

fun Resource.appendPractitionerInfo(practitionerId: String?) {
  practitionerId?.let {
    // Convert practitioner uuid to reference e.g. "Practitioner/some-gibberish-uuid"
    val practitionerRef = it.asReference(ResourceType.Practitioner)

    when (this) {
      is Patient ->
        generalPractitioner =
          if (generalPractitioner.isNullOrEmpty()) {
            arrayListOf(practitionerRef)
          } else {
            generalPractitioner
          }
      is Observation ->
        performer = if (performer.isNullOrEmpty()) arrayListOf(practitionerRef) else performer
      is QuestionnaireResponse -> author = updateReference(author, practitionerRef)
      is Flag -> author = updateReference(author, practitionerRef)
      is Encounter ->
        participant =
          if (participant.isNullOrEmpty()) {
            arrayListOf(
              Encounter.EncounterParticipantComponent().apply { individual = practitionerRef },
            )
          } else {
            participant
          }
      is Consent -> performer = updateReferenceList(performer, practitionerRef)
      else -> {}
    }
  }
}

fun Resource.appendRelatedEntityLocation(
  questionnaireResponse: QuestionnaireResponse,
  questionnaireConfig: QuestionnaireConfig,
  context: Context,
) {
  val locationCoding =
    Coding().apply {
      system =
        context.getString(
          org.smartregister.fhircore.engine.R.string.sync_strategy_related_entity_location_system,
        )
      display =
        context.getString(
          org.smartregister.fhircore.engine.R.string.sync_strategy_related_entity_location_display,
        )
    }
  questionnaireConfig.linkIds
    ?.filter { it.type == LinkIdType.LOCATION }
    ?.forEach { linkIdConfig ->
      val answer: Type? = questionnaireResponse.find(linkIdConfig.linkId)?.answerFirstRep?.value
      val locationId =
        when (answer) {
          is Reference -> answer.reference.extractLogicalIdUuid()
          is StringType -> answer.value.extractLogicalIdUuid()
          else -> null
        }
      val existingTag = this.meta.getTag(locationCoding.system, locationId)
      if (!locationId.isNullOrEmpty() && existingTag == null) {
        this.meta.addTag(locationCoding.apply { setCode(locationId) })
      }
    }
}

private fun updateReferenceList(
  oldReferenceList: List<Reference>?,
  newReference: Reference,
): List<Reference> {
  val list = oldReferenceList?.filter { !it.reference.isNullOrEmpty() }
  return if (!list.isNullOrEmpty()) list else listOf(newReference)
}

private fun updateReference(oldReference: Reference?, newReference: Reference): Reference =
  if (oldReference == null || oldReference.reference.isNullOrEmpty()) {
    newReference
  } else {
    Reference(oldReference.reference)
  }

fun Resource.updateLastUpdated() {
  meta.lastUpdated = Date()
}

fun Resource.asReference() = Reference().apply { this.reference = "$resourceType/$logicalId" }

fun Resource.referenceValue(): String = "$resourceType/$logicalId"

fun Resource.referenceParamForCondition(): ReferenceClientParam =
  when (resourceType) {
    ResourceType.Patient -> Condition.PATIENT
    ResourceType.Encounter -> Condition.ENCOUNTER
    else ->
      throw IllegalStateException("Do not know how to use $resourceType for Condition resource")
  }

fun Resource.referenceParamForObservation(): ReferenceClientParam =
  when (resourceType) {
    ResourceType.Patient -> Observation.PATIENT
    ResourceType.Encounter -> Observation.ENCOUNTER
    ResourceType.QuestionnaireResponse -> Observation.FOCUS
    else ->
      throw IllegalStateException("Do not know how to use $resourceType for Observation resource")
  }

fun Resource.setPropertySafely(name: String, value: Base) =
  kotlin.runCatching { this.setProperty(name, value) }.onFailure { Timber.w(it) }.getOrNull()

fun isValidResourceType(resourceCode: String): Boolean {
  return try {
    ResourceType.fromCode(resourceCode)
    true
  } catch (exception: FHIRException) {
    false
  }
}

fun ImplementationGuide.retrieveImplementationGuideDefinitionResources():
  List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> {
  val resources =
    mutableListOf<ImplementationGuide.ImplementationGuideDefinitionResourceComponent>()
  this.definition.resource.forEach { resources.add(it) }
  return resources
}

/**
 * Composition sections can be nested. This function retrieves all the nested composition sections
 * and returns a flattened list of all [Composition.SectionComponent] for the given [Composition]
 * resource
 */
fun Composition.retrieveCompositionSections(): List<Composition.SectionComponent> {
  val sections = mutableListOf<Composition.SectionComponent>()
  val sectionsQueue = ArrayDeque<Composition.SectionComponent>()
  this.section.forEach {
    if (!it.section.isNullOrEmpty()) {
      it.section.forEach { sectionComponent -> sectionsQueue.addLast(sectionComponent) }
    }
    sections.add(it)
  }
  while (sectionsQueue.isNotEmpty()) {
    val sectionComponent = sectionsQueue.removeFirst()
    if (!sectionComponent.section.isNullOrEmpty()) {
      sectionComponent.section.forEach { sectionsQueue.addLast(it) }
    }
    sections.add(sectionComponent)
  }
  return sections
}

fun String.resourceClassType(): Class<out Resource> =
  FhirContext.forR4Cached().getResourceDefinition(this).implementingClass as Class<out Resource>

/**
 * A function that extracts only the UUID part of a resource logicalId.
 *
 * Examples:
 * 1. "Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4/_history/2" returns
 *
 * ```
 *    "0acda8c9-3fa3-40ae-abcd-7d1fba7098b4".
 * ```
 * 2. "Group/0acda8c9-3fa3-40ae-abcd-7d1fba7098b4" returns "0acda8c9-3fa3-40ae-abcd-7d1fba7098b4".
 */
fun String.extractLogicalIdUuid() = this.substringAfter("/").substringBefore("/")

/**
 * This suspend function updates the due date of the dependents of the current [Task], based on the
 * date of a related [Immunization] [Task]. The function loops through all the tasks that are
 * part-of the current task, loads the dependent tasks and their related immunization resources from
 * the [DefaultRepository] then updates the start date of the dependent task if it's scheduled to
 * start before the immunization date plus the required number of days.
 *
 * This function can be extended in future to support other [ResourceType] s.
 */
suspend fun Task.updateDependentTaskDueDate(
  defaultRepository: DefaultRepository,
): Task {
  return apply {
    val dependentTasks =
      defaultRepository.fhirEngine
        .batchedSearch<Task> {
          filter(
            referenceParameter = ReferenceClientParam(PARTOF),
            { value = id },
          )
        }
        .map { it.resource }
    dependentTasks.forEach { dependantTask ->
      dependantTask.partOf.forEach { _ ->
        if (
          dependantTask.executionPeriod.hasStart() &&
            dependantTask.hasInput() &&
            dependantTask.status.equals(
              Task.TaskStatus.REQUESTED,
            )
        ) {
          this.output.forEach { taskOp ->
            try {
              val taskOutReference = taskOp.value as Reference
              if (taskOp.value != null) {
                if (taskOutReference.extractType()?.equals(ResourceType.Immunization) == true) {
                  val immunizationRef = taskOutReference.reference
                  val immunization =
                    defaultRepository.fhirEngine.get<Immunization>(
                      immunizationRef.extractLogicalIdUuid(),
                    )
                  if (immunization.isResource && immunization.hasOccurrence()) {
                    val dependentTaskStartDate = dependantTask.executionPeriod.start
                    val immunizationDate =
                      Instant.parse(immunization.occurrence.valueToString()).toDate()
                    dependantTask.input.onEach { input ->
                      if (input.value.isPrimitive) {
                        val dependentTaskInputDuration = input.value.valueToString().toInt()
                        val difference =
                          abs(
                            Duration.between(
                                immunizationDate.toInstant(),
                                dependentTaskStartDate.toInstant(),
                              )
                              .toDays(),
                          )
                        if (difference < dependentTaskInputDuration) {
                          dependantTask
                            .apply {
                              executionPeriod.start =
                                Date.from(
                                  immunizationDate
                                    ?.toInstant()
                                    ?.plus(dependentTaskInputDuration.toLong(), ChronoUnit.DAYS),
                                )
                            }
                            .run {
                              defaultRepository.addOrUpdate(
                                addMandatoryTags = true,
                                resource = dependantTask,
                              )
                            }
                        }
                      }
                    }
                  }
                }
              }
            } catch (ex: Exception) {
              Timber.e(ex)
            }
          }
        }
      }
    }
  }
}

/**
 * Filter provided [Resource]'s using FhirPath expressions. The extracted FHIRPath value is REQUIRED
 * to be a boolean otherwise the [toBoolean] function will evaluate to false and hence return an
 * empty list.
 */
fun List<RepositoryResourceData>.filterByFhirPathExpression(
  fhirPathDataExtractor: FhirPathDataExtractor,
  conditionalFhirPathExpressions: List<String>?,
  matchAll: Boolean,
): List<RepositoryResourceData> {
  if (conditionalFhirPathExpressions.isNullOrEmpty()) return this
  return this.filter { repositoryResourceData ->
    if (matchAll) {
      conditionalFhirPathExpressions.all {
        fhirPathDataExtractor.extractValue(repositoryResourceData.resource, it).toBoolean()
      }
    } else {
      conditionalFhirPathExpressions.any {
        fhirPathDataExtractor.extractValue(repositoryResourceData.resource, it).toBoolean()
      }
    }
  }
}

/** Extracts and returns a translated string for the gender in the resource */
fun Resource.extractGender(context: Context): String {
  return when (this) {
    is Patient -> getGenderString(this.gender, context)
    is RelatedPerson -> getGenderString(this.gender, context)
    else -> ""
  }
}

private fun getGenderString(gender: Enumerations.AdministrativeGender?, context: Context): String {
  return when (gender) {
    Enumerations.AdministrativeGender.MALE -> context.getString(R.string.male)
    Enumerations.AdministrativeGender.FEMALE -> context.getString(R.string.female)
    Enumerations.AdministrativeGender.OTHER -> context.getString(R.string.other)
    Enumerations.AdministrativeGender.UNKNOWN -> context.getString(R.string.unknown)
    else -> ""
  }
}

fun Enumerations.AdministrativeGender.translateGender(context: Context) =
  when (this) {
    Enumerations.AdministrativeGender.MALE -> context.getString(R.string.male)
    Enumerations.AdministrativeGender.FEMALE -> context.getString(R.string.female)
    else -> context.getString(R.string.unknown)
  }

/** Extract a Resource's age if birthDate is an available field */
fun Resource.extractAge(context: Context): String {
  return when (this) {
    is Patient -> this.birthDate?.let { calculateAge(it, context) } ?: ""
    is RelatedPerson -> this.birthDate?.let { calculateAge(it, context) } ?: ""
    else -> ""
  }
}

/** Extract a Resource's birthDate if it's an available field */
fun Resource.extractBirthDate(): Date? {
  return when (this) {
    is Patient -> this.birthDate
    is RelatedPerson -> this.birthDate
    else -> null
  }
}
