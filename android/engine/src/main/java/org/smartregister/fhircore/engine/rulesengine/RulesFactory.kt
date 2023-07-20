/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.rulesengine

import android.content.Context
import com.google.android.fhir.logicalId
import com.google.android.fhir.search.Order
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.Task
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.Rules
import org.joda.time.DateTime
import org.ocpsoft.prettytime.PrettyTime
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RelatedResourceCount
import org.smartregister.fhircore.engine.domain.model.RepositoryResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.SDF_E_MMM_DD_YYYY
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.formatDate
import org.smartregister.fhircore.engine.util.extension.isOverDue
import org.smartregister.fhircore.engine.util.extension.parseDate
import org.smartregister.fhircore.engine.util.extension.prettifyDate
import org.smartregister.fhircore.engine.util.extension.translationPropertyKey
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.helper.LocalizationHelper
import timber.log.Timber

class RulesFactory
@Inject
constructor(
  @ApplicationContext val context: Context,
  val configurationRegistry: ConfigurationRegistry,
  val fhirPathDataExtractor: FhirPathDataExtractor,
  val dispatcherProvider: DispatcherProvider,
) : RulesListener() {
  val rulesEngineService = RulesEngineService()
  private var facts: Facts = Facts()

  /**
   * This function executes the actions defined in the [Rule] s generated from the provided list of
   * [RuleConfig] against the [Facts] populated by the provided FHIR [Resource] s available in the
   * [RepositoryResourceData.resource], [RepositoryResourceData.relatedResourcesMap] and
   * [RepositoryResourceData.relatedResourcesCountMap]. All related resources of same type are
   * flattened in a map for ease of usage in the rule engine.
   */
  fun fireRules(rules: Rules, repositoryResourceData: RepositoryResourceData?): Map<String, Any> {
    facts =
      Facts().apply {
        put(FHIR_PATH, fhirPathDataExtractor)
        put(DATA, mutableMapOf<String, Any>())
        put(SERVICE, rulesEngineService)
      }
    if (repositoryResourceData != null) {
      with(repositoryResourceData) {
        facts.apply {
          put(resourceRulesEngineFactId ?: resource.resourceType.name, resource)
          relatedResourcesMap.addToFacts(this)
          relatedResourcesCountMap.addToFacts(this)

          // Populate the facts map with secondary resource data flatten base and related
          // resources
          secondaryRepositoryResourceData
            ?.groupBy { it.resourceRulesEngineFactId ?: it.resource.resourceType.name }
            ?.forEach { entry -> put(entry.key, entry.value.map { it.resource }) }

          secondaryRepositoryResourceData?.forEach { repoResourceData ->
            repoResourceData.relatedResourcesMap.forEach { entry ->
              val existingRelatedResourceList = get<MutableList<Resource>>(entry.key)
              if (existingRelatedResourceList == null) {
                put(entry.key, mutableListOf<Resource>())
              }
              get<MutableList<Resource>>(entry.key).addAll(entry.value)
            }

            repoResourceData.relatedResourcesCountMap.forEach { entry ->
              val existingRelatedResourceCountList =
                get<MutableList<RelatedResourceCount>>(entry.key)
              if (existingRelatedResourceCountList == null) {
                put(entry.key, mutableListOf<RelatedResourceCount>())
              }
              get<MutableList<RelatedResourceCount>>(entry.key).addAll(entry.value)
            }
          }
        }
      }
    }
    if (BuildConfig.DEBUG) {
      val timeToFireRules = measureTimeMillis { rulesEngine.fire(rules, facts) }
      Timber.d("Rule executed in $timeToFireRules millisecond(s)")
    } else rulesEngine.fire(rules, facts)
    return facts.get(DATA) as Map<String, Any>
  }

  /** Provide access to utility functions accessible to the users defining rules in JSON format. */
  inner class RulesEngineService {

    /**
     * This function creates a property key from the string [value] and uses the key to retrieve the
     * correct translation from the string.properties file.
     */
    fun translate(value: String): String =
      configurationRegistry.localizationHelper.parseTemplate(
        LocalizationHelper.STRINGS_BASE_BUNDLE_NAME,
        Locale.getDefault(),
        "{{${value.translationPropertyKey()}}}",
      )

    /**
     * This method retrieves a list of relatedResources for a given resource from the facts map It
     * fetches a list of facts of the given [relatedResourceKey] then iterates through this list in
     * order to return a list of all resources whose subject reference matches the logical Id of the
     * [resource]
     *
     * @param resource The parent resource for which the related resources will be retrieved
     * @param relatedResourceKey The key representing the relatedResources in the map
     * @param referenceFhirPathExpression A fhir path expression used to retrieve the subject
     *   reference Id from the related resources
     */
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun retrieveRelatedResources(
      resource: Resource,
      relatedResourceKey: String,
      referenceFhirPathExpression: String?,
      relatedResourcesMap: Map<String, List<Resource>>? = null,
    ): List<Resource> {
      val value: List<Resource> =
        relatedResourcesMap?.get(relatedResourceKey)
          ?: if (facts.getFact(relatedResourceKey) != null) {
            facts.getFact(relatedResourceKey).value as List<Resource>
          } else {
            emptyList()
          }

      return if (referenceFhirPathExpression.isNullOrEmpty()) {
        value
      } else
        value.filter {
          resource.logicalId ==
            fhirPathDataExtractor
              .extractValue(it, referenceFhirPathExpression)
              .extractLogicalIdUuid()
        }
    }

    /**
     * This method retrieve a parentResource for a given relatedResource from the facts map It
     * fetches a list of facts of the given [parentResourceType] then iterates through this list in
     * order to return a resource whose logical id matches the subject reference retrieved via
     * fhirPath from the [childResource]
     * - The logical Id of the parentResource [parentResourceType]
     * - The ResourceType the parentResources belong to [fhirPathExpression]
     * - A fhir path expression used to retrieve the logical Id from the parent resources
     */
    @Suppress("UNCHECKED_CAST")
    fun retrieveParentResource(
      childResource: Resource,
      parentResourceType: String,
      fhirPathExpression: String,
    ): Resource? {
      val value = facts.getFact(parentResourceType).value as List<Resource>
      val parentResourceId =
        fhirPathDataExtractor.extractValue(childResource, fhirPathExpression).extractLogicalIdUuid()
      return value.find { it.logicalId == parentResourceId }
    }

    /**
     * This function returns a true or false value if any ( [matchAll]= false) or all ( [matchAll]=
     * true) of the [resources] satisfy the [fhirPathExpression] provided
     *
     * [resources] List of resources the expressions are run against [fhirPathExpression] An
     * expression to run against the provided resources [matchAll] When true the function checks
     * whether all of the resources fulfill the expression provided
     *
     * ```
     *            When false the function checks whether any of the resources fulfills the expression provided
     * ```
     */
    @JvmOverloads
    fun evaluateToBoolean(
      resources: List<Resource>?,
      fhirPathExpression: String,
      matchAll: Boolean = false,
    ): Boolean =
      if (matchAll) {
        resources?.all { base ->
          fhirPathDataExtractor.extractData(base, fhirPathExpression).any {
            it.isBooleanPrimitive && it.primitiveValue().toBoolean()
          }
        }
          ?: false
      } else {
        resources?.any { base ->
          fhirPathDataExtractor.extractData(base, fhirPathExpression).any {
            it.isBooleanPrimitive && it.primitiveValue().toBoolean()
          }
        }
          ?: false
      }

    /**
     * This function transform the provided [resources] into a list of [label] given that the
     * [fhirPathExpression] for each of the resources is evaluated to true.
     *
     * Example: To retrieve a list of household member icons, find Patients aged 5yrs and below then
     * return Comma Separated Values of 'CHILD' (to be serialized into [ServiceMemberIcon]) for
     * each.
     */
    fun mapResourcesToLabeledCSV(
      resources: List<Resource>?,
      fhirPathExpression: String,
      label: String,
    ): String =
      resources
        ?.mapNotNull {
          if (
            fhirPathDataExtractor.extractData(it, fhirPathExpression).any { base ->
              base.isBooleanPrimitive && base.primitiveValue().toBoolean()
            }
          ) {
            label
          } else {
            null
          }
        }
        ?.distinctBy { it }
        ?.joinToString(",")
        ?: ""

    /**
     * Transforms a [resource] into [label] if the [fhirPathExpression] is evaluated to true.
     *
     * Example: To retrieve the icon for household member who is a child, evaluate their age to be
     * less than 5years, if 'true' return 'CHILD' (to be serialized to [ServiceMemberIcon])
     */
    fun mapResourceToLabeledCSV(
      resource: Resource,
      fhirPathExpression: String,
      label: String,
    ): String = mapResourcesToLabeledCSV(listOf(resource), fhirPathExpression, label)

    /** This function extracts the patient's age from the patient resource */
    fun extractAge(patient: Patient): String = patient.extractAge(context)

    /**
     * This function extracts and returns a translated string for the gender in Patient resource.
     */
    fun extractGender(patient: Patient): String = patient.extractGender(context) ?: ""

    /** This function extracts the patient's DOB from the FHIR resource */
    fun extractDOB(patient: Patient, dateFormat: String): String =
      SimpleDateFormat(dateFormat, Locale.ENGLISH).run { format(patient.birthDate) }

    /**
     * This function takes [inputDate] and returns a difference (for examples 7 hours, 2 day, 5
     * months, 3 years etc)
     */
    fun prettifyDate(inputDate: Date): String = inputDate.prettifyDate()

    /**
     * This function takes [inputDateString] like 2022-7-1 and returns a difference (for examples 7
     * hours ago, 2 days ago, 5 months ago, 3 years ago etc) [inputDateString] can give given as
     * 2022-02 or 2022
     */
    fun prettifyDate(inputDateString: String): String {
      return PrettyTime().format(DateTime(inputDateString).toDate())
    }

    /**
     * This function is responsible for formatting a date for whatever expectedFormat we need. It
     * takes an [inputDate] string along with the [inputDateFormat] so it can convert it to the Date
     * and then it gives output in expected Format, [expectedFormat] is by default (Example: Mon,
     * Nov 5 2021)
     */
    @JvmOverloads
    fun formatDate(
      inputDate: String,
      inputDateFormat: String,
      expectedFormat: String = SDF_E_MMM_DD_YYYY,
    ): String? = inputDate.parseDate(inputDateFormat)?.formatDate(expectedFormat)

    /**
     * This function is responsible for formatting a date for whatever expectedFormat we need. It
     * takes an input a [date] as input and then it gives output in expected Format,
     * [expectedFormat] is by default (Example: Mon, Nov 5 2021)
     */
    @JvmOverloads
    fun formatDate(date: Date, expectedFormat: String = SDF_E_MMM_DD_YYYY): String =
      date.formatDate(expectedFormat)

    /**
     * This function generates a random 6-digit integer between a hard-coded range. It may generate
     * duplicate outputs on subsequent function calls.
     *
     * @return An Integer.
     */
    fun generateRandomSixDigitInt(): Int =
      (INCLUSIVE_SIX_DIGIT_MINIMUM..INCLUSIVE_SIX_DIGIT_MAXIMUM).random()

    /**
     * This function filters resources provided the condition extracted from the
     * [fhirPathExpression] is met
     */
    fun filterResources(resources: List<Resource>?, fhirPathExpression: String): List<Resource> {
      if (fhirPathExpression.isEmpty()) {
        return emptyList()
      }
      return resources?.filter {
        fhirPathDataExtractor.extractValue(it, fhirPathExpression).toBoolean()
      }
        ?: emptyList()
    }

    /**
     * This function combines all string indexes to a list separated by the separator and regex
     * defined by the content author
     */
    @JvmOverloads
    fun joinToString(
      sourceString: MutableList<String?>,
      regex: String = DEFAULT_REGEX,
      separator: String = DEFAULT_STRING_SEPARATOR,
    ): String {
      sourceString.removeIf { it == null }
      val inputString = sourceString.joinToString()
      return regex.toRegex().findAll(inputString).joinToString(separator) { it.groupValues[1] }
    }

    /** This function returns a list of resources with a limit of [limit] resources */
    fun limitTo(source: List<Any>?, limit: Int?): List<Any> {
      if (limit == null || limit <= 0) {
        return emptyList()
      }
      return source?.take(limit) ?: emptyList()
    }

    fun mapResourcesToExtractedValues(
      resources: List<Resource>?,
      fhirPathExpression: String,
    ): List<Any> {
      if (fhirPathExpression.isEmpty()) {
        return emptyList()
      }
      return resources?.map { fhirPathDataExtractor.extractValue(it, fhirPathExpression) }
        ?: emptyList()
    }

    fun computeTotalCount(relatedResourceCounts: List<RelatedResourceCount>?): Long =
      relatedResourceCounts?.sumOf { it.count } ?: 0

    fun retrieveCount(
      parentResourceId: String,
      relatedResourceCounts: List<RelatedResourceCount>?,
    ): Long =
      relatedResourceCounts
        ?.find { parentResourceId.equals(it.parentResourceId, ignoreCase = true) }
        ?.count
        ?: 0

    /**
     * This function sorts [resources] by comparing the values extracted by FHIRPath using the
     * [fhirPathExpression]. The [dataType] is required for ordering of the. You can optionally
     * specify the [Order] of sorting.
     */
    @JvmOverloads
    fun sortResources(
      resources: List<Resource>?,
      fhirPathExpression: String,
      dataType: String,
      order: String = Order.ASCENDING.name,
    ): List<Resource>? {
      val mappedResources =
        resources?.mapNotNull {
          val extractedValue: Base? =
            fhirPathDataExtractor.extractData(it, fhirPathExpression).firstOrNull()
          val sortingValue: Comparable<*>? =
            when (DataType.valueOf(dataType)) {
              DataType.BOOLEAN -> extractedValue?.castToBoolean(extractedValue)?.value
              DataType.DATE -> extractedValue?.castToDate(extractedValue)?.value
              DataType.DATETIME -> extractedValue?.castToDateTime(extractedValue)?.value
              DataType.DECIMAL -> extractedValue?.castToDecimal(extractedValue)?.value
              DataType.INTEGER -> extractedValue?.castToInteger(extractedValue)?.value
              DataType.STRING -> extractedValue?.castToString(extractedValue)?.value
              else -> {
                Timber.e(
                  "Sorting only works for primitive types, sorting by the data type $dataType is not allowed. Implement sorting strategy for the data type $dataType.",
                )
                null
              }
            }
          if (sortingValue != null) Pair(sortingValue, it) else null
        }

      return when (Order.valueOf(order)) {
        Order.ASCENDING -> mappedResources?.sortedWith(compareBy { it.first })?.map { it.second }
        Order.DESCENDING ->
          mappedResources?.sortedWith(compareByDescending { it.first })?.map { it.second }
      }
    }

    fun generateTaskServiceStatus(task: Task): String {
      val serviceStatus: String
      if (task.isOverDue()) {
        serviceStatus = ServiceStatus.OVERDUE.name
      } else {
        serviceStatus =
          when (task.status) {
            Task.TaskStatus.NULL,
            Task.TaskStatus.FAILED,
            Task.TaskStatus.RECEIVED,
            Task.TaskStatus.ENTEREDINERROR,
            Task.TaskStatus.ACCEPTED,
            Task.TaskStatus.REJECTED,
            Task.TaskStatus.DRAFT,
            Task.TaskStatus.ONHOLD, -> {
              Timber.e("Task.status is null", Exception())
              ServiceStatus.UPCOMING.name
            }
            Task.TaskStatus.REQUESTED -> ServiceStatus.UPCOMING.name
            Task.TaskStatus.READY -> ServiceStatus.DUE.name
            Task.TaskStatus.CANCELLED -> ServiceStatus.EXPIRED.name
            Task.TaskStatus.INPROGRESS -> ServiceStatus.IN_PROGRESS.name
            Task.TaskStatus.COMPLETED -> ServiceStatus.COMPLETED.name
          }
      }
      return serviceStatus
    }
  }

  companion object {

    private const val SERVICE = "service"
    private const val INCLUSIVE_SIX_DIGIT_MINIMUM = 100000
    private const val INCLUSIVE_SIX_DIGIT_MAXIMUM = 999999
    private const val DEFAULT_REGEX = "(?<=^|,)[\\s,]*(\\w[\\w\\s]*)(?=[\\s,]*$|,)"
    private const val DEFAULT_STRING_SEPARATOR = ", "
  }
}
