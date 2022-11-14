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

package org.smartregister.fhircore.engine.rulesengine

import android.content.Context
import com.google.android.fhir.logicalId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlException
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.codesystems.AdministrativeGender
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.jexl.JexlRule
import org.joda.time.DateTime
import org.ocpsoft.prettytime.PrettyTime
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
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
  val fhirPathDataExtractor: FhirPathDataExtractor
) : RuleListener {

  val rulesEngineService = RulesEngineService()
  private var facts: Facts = Facts()
  private val rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
  private val computedValuesMap = mutableMapOf<String, Any>()
  private val jexlEngine =
    JexlBuilder()
      .namespaces(
        mutableMapOf<String, Any>(
          "Timber" to Timber,
          "StringUtils" to Class.forName("org.apache.commons.lang3.StringUtils"),
          "RegExUtils" to Class.forName("org.apache.commons.lang3.RegExUtils")
        )
      )
      .silent(false)
      .strict(false)
      .create()

  init {
    rulesEngine.registerRuleListener(this)
  }

  override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

  override fun onSuccess(rule: Rule, facts: Facts) {
    if (BuildConfig.DEBUG) {
      Timber.d("Rule executed: %s -> %s", rule, computedValuesMap[rule.name])
    }
  }

  override fun onFailure(rule: Rule, facts: Facts, exception: Exception?) =
    if (exception is JexlException) {
      when (exception) {
        // Just display error message for undefined variable; expected for missing facts
        is JexlException.Variable ->
          logWarning(
            "${exception.localizedMessage}, consider checking for null before usage: e.g ${exception.variable} != null"
          )
        else -> Timber.e(exception)
      }
    } else {
      logError(exception)
    }

  override fun onEvaluationError(rule: Rule, facts: Facts, exception: java.lang.Exception) {
    logError("Evaluation error", exception)
  }

  override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) = Unit

  /**
   * This function executes the actions defined in the [Rule] s generated from the provided list of
   * [RuleConfig] against the [Facts] populated by the provided FHIR [Resource] s available in the
   * [relatedResourcesMap] and the [baseResource].
   */
  fun fireRule(
    ruleConfigs: List<RuleConfig>,
    baseResource: Resource,
    relatedResourcesMap: Map<String, List<Resource>> = emptyMap(),
  ): Map<String, Any> {
    // Reset previously computed values and init facts
    computedValuesMap.clear()
    facts.apply {
      clear()
      put(FHIR_PATH, fhirPathDataExtractor)
      put(DATA, computedValuesMap)
      put(SERVICE, rulesEngineService)
    }

    val customRules = mutableSetOf<Rule>()
    ruleConfigs.forEach { ruleConfig ->

      // Create JEXL rule
      val customRule: JexlRule =
        JexlRule(jexlEngine)
          .name(ruleConfig.name)
          .description(ruleConfig.description)
          .`when`(ruleConfig.condition.ifEmpty { TRUE })

      ruleConfig.actions.forEach { customRule.then(it) }
      customRules.add(customRule)
    }

    // baseResource is a FHIR resource whereas relatedResources is a list of FHIR resources
    facts.put(baseResource.resourceType.name, baseResource)
    relatedResourcesMap.forEach { facts.put(it.key, it.value) }

    rulesEngine.fire(Rules(customRules), facts)

    return mutableMapOf<String, Any>().apply { putAll(computedValuesMap) }
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
        "{{${value.translationPropertyKey()}}}"
      )

    /**
     * This method retrieves a list of relatedResources for a given resource from the facts map It
     * fetches a list of facts of the given [relatedResourceType] then iterates through this list in
     * order to return a list of all resources whose subject reference matches the logical Id of the
     * [resource]
     *
     * [resource]
     * - The parent resource for which the related resources will be retrieved [relatedResourceType]
     * - The ResourceType the relatedResources belong to [fhirPathExpression]
     * - A fhir path expression used to retrieve the subject reference Id from the related resources
     */
    @Suppress("UNCHECKED_CAST")
    fun retrieveRelatedResources(
      resource: Resource,
      relatedResourceType: String,
      fhirPathExpression: String,
      resourceData: ResourceData? = null
    ): List<Resource> {
      val value: List<Resource> =
        resourceData?.relatedResourcesMap?.get(relatedResourceType)
          ?: if (facts.getFact(relatedResourceType) != null)
            facts.getFact(relatedResourceType).value as List<Resource>
          else emptyList()

      return value.filter {
        resource.logicalId ==
          fhirPathDataExtractor.extractValue(it, fhirPathExpression).extractLogicalIdUuid()
      }
    }

    /**
     * This method retrieve a parentResource for a given relatedResource from the facts map It
     * fetches a list of facts of the given [parentResourceType] then iterates through this list in
     * order to return a resource whose logical id matches the subject reference retrieved via
     * fhirPath from the [childResource]
     *
     * - The logical Id of the parentResource [parentResourceType]
     * - The ResourceType the parentResources belong to [fhirPathExpression]
     * - A fhir path expression used to retrieve the logical Id from the parent resources
     */
    @Suppress("UNCHECKED_CAST")
    fun retrieveParentResource(
      childResource: Resource,
      parentResourceType: String,
      fhirPathExpression: String
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
     * ```
     *            When false the function checks whether any of the resources fulfills the expression provided
     * ```
     */
    fun evaluateToBoolean(
      resources: List<Resource>?,
      fhirPathExpression: String,
      matchAll: Boolean = false
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
      label: String
    ): String? =
      resources
        ?.mapNotNull {
          if (fhirPathDataExtractor.extractData(it, fhirPathExpression).any { base ->
              base.isBooleanPrimitive && base.primitiveValue().toBoolean()
            }
          ) {
            label
          } else null
        }
        ?.joinToString(",")

    /**
     * Transforms a [resource] into [label] if the [fhirPathExpression] is evaluated to true.
     *
     * Example: To retrieve the icon for household member who is a child, evaluate their age to be
     * less than 5years, if 'true' return 'CHILD' (to be serialized to [ServiceMemberIcon])
     */
    fun mapResourceToLabeledCSV(
      resource: Resource,
      fhirPathExpression: String,
      label: String
    ): String? = mapResourcesToLabeledCSV(listOf(resource), fhirPathExpression, label)

    /** This function extracts the patient's age from the patient resource */
    fun extractAge(patient: Patient): String = patient.extractAge()

    /**
     * This function extracts the gender from patient's reosurce.
     *
     * It the returns strings representation of the age.
     */
    fun extractGender(patient: Patient): String {
      return if (patient.hasGender()) {
        when (AdministrativeGender.valueOf(patient.gender.name)) {
          AdministrativeGender.MALE -> context.getString(R.string.male)
          AdministrativeGender.FEMALE -> context.getString(R.string.female)
          AdministrativeGender.OTHER -> context.getString(R.string.other)
          AdministrativeGender.UNKNOWN -> context.getString(R.string.unknown)
          AdministrativeGender.NULL -> ""
        }
      } else ""
    }

    /** This function extracts the patient's DOB from the FHIR resource */
    fun extractDOB(patient: Patient, dateFormat: String): String =
      SimpleDateFormat(dateFormat, Locale.ENGLISH).run { format(patient.birthDate) }

    /**
     * This function takes [inputDate] and returns a difference (for examples 7 hours ago, 2 days
     * ago, 5 months ago, 3 years ago etc)
     */
    fun convertDateForDifference(inputDate: Date): String {
      return inputDate.prettifyDate()
    }

    /**
     * This function takes [inputDateString] like 2022-7-1 and returns a difference (for examples 7
     * hours ago, 2 days ago, 5 months ago, 3 years ago etc) [inputDateString] can give given as
     * 2022-02 or 2022
     */
    fun convertDateForDifference(inputDateString: String): String {
      return PrettyTime(Locale.getDefault()).format(DateTime(inputDateString).toDate())
    }

    /**
     * This function is responsible for formatting a date for whatever expectedFormat we need. It
     * takes an [inputDate] string along with the [inputDateFormat] so it can convert it to the Date
     * and then it gives output in expected Format, [expectedFormat] is by default (Example: Mon,
     * Nov 5 2021)
     */
    fun formatDate(
      inputDate: String,
      inputDateFormat: String,
      expectedFormat: String = "E, MMM dd yyyy"
    ): String {
      return SimpleDateFormat(expectedFormat, Locale.getDefault()).run {
        format(SimpleDateFormat(inputDateFormat, Locale.getDefault()).parse(inputDate))
      }
    }

    /**
     * This function is responsible for formatting a date for whatever expectedFormat we need. It
     * takes an input a [date] as input and then it gives output in expected Format,
     * [expectedFormat] is by default (Example: Mon, Nov 5 2021)
     */
    fun formatDate(date: Date, expectedFormat: String = "E, MMM dd yyyy"): String {
      return SimpleDateFormat(expectedFormat, Locale.getDefault()).run { format(date) }
    }
  }

  fun logWarning(message: String) {
    Timber.d(message)
  }

  fun logError(exception: Exception?) {
    Timber.e(exception)
  }

  fun logError(message: String, exception: Exception?) {
    Timber.e(message, exception)
  }

  companion object {
    private const val FHIR_PATH = "fhirPath"
    private const val DATA = "data"
    private const val TRUE = "true"
    private const val SERVICE = "service"
  }
}
