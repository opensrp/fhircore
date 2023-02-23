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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.withContext
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlException
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.jexl.JexlRule
import org.joda.time.DateTime
import org.ocpsoft.prettytime.PrettyTime
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.domain.model.ServiceMemberIcon
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.extension.extractAge
import org.smartregister.fhircore.engine.util.extension.extractGender
import org.smartregister.fhircore.engine.util.extension.extractLogicalIdUuid
import org.smartregister.fhircore.engine.util.extension.formatDate
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
  val dispatcherProvider: DispatcherProvider
) : RuleListener {

  val rulesEngineService = RulesEngineService()
  private val rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
  private val jexlEngine =
    JexlBuilder()
      .namespaces(
        mutableMapOf<String, Any>(
          "Timber" to Timber,
          "StringUtils" to Class.forName("org.apache.commons.lang3.StringUtils"),
          "RegExUtils" to Class.forName("org.apache.commons.lang3.RegExUtils"),
          "Math" to Class.forName("java.lang.Math")
        )
      )
      .silent(false)
      .strict(false)
      .create()

  private var facts: Facts = Facts()

  init {
    rulesEngine.registerRuleListener(this)
  }

  override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

  override fun onSuccess(rule: Rule, facts: Facts) {
    if (BuildConfig.DEBUG) {
      val computedValuesMap = facts.get(DATA) as Map<String, Any>
      Timber.d("Rule executed: %s -> %s", rule, computedValuesMap[rule.name])
    }
  }

  override fun onFailure(rule: Rule, facts: Facts, exception: Exception) =
    if (exception is JexlException) {
      when (exception) {
        // Just display error message for undefined variable; expected for missing facts
        is JexlException.Variable ->
          log(
            exception,
            "${exception.localizedMessage}, consider checking for null before usage: e.g ${exception.variable} != null"
          )
        else -> log(exception)
      }
    } else log(exception)

  override fun onEvaluationError(rule: Rule, facts: Facts, exception: java.lang.Exception) {
    log(exception, "Evaluation error")
  }

  override fun afterEvaluate(rule: Rule, facts: Facts, evaluationResult: Boolean) = Unit

  fun log(exception: java.lang.Exception, message: String? = null) = Timber.e(exception, message)

  /**
   * This function executes the actions defined in the [Rule] s generated from the provided list of
   * [RuleConfig] against the [Facts] populated by the provided FHIR [Resource] s available in the
   * [relatedResourcesMap] and the [baseResource].
   */
  @Suppress("UNCHECKED_CAST")
  suspend fun fireRules(
    rules: Rules,
    baseResource: Resource? = null,
    relatedResourcesMap: Map<String, List<Resource>> = emptyMap(),
  ): Map<String, Any> {
    return withContext(dispatcherProvider.io()) {
      // Initialize new facts and fire rules in background
      facts =
        Facts().apply {
          put(FHIR_PATH, fhirPathDataExtractor)
          put(DATA, mutableMapOf<String, Any>())
          put(SERVICE, rulesEngineService)
          if (baseResource != null) {
            put(baseResource.resourceType.name, baseResource)
          }
          relatedResourcesMap.forEach { put(it.key, it.value) }
        }
      val timeToFireRules = measureTimeMillis { rulesEngine.fire(rules, facts) }
      Timber.d("Rule executed in $timeToFireRules millisecond(s)")
      facts.get(DATA) as Map<String, Any>
    }
  }

  fun generateRules(ruleConfigs: List<RuleConfig>): Rules {
    val customRules =
      ruleConfigs
        .map { ruleConfig ->
          val customRule: JexlRule =
            JexlRule(jexlEngine)
              .name(ruleConfig.name)
              .description(ruleConfig.description)
              .priority(ruleConfig.priority)
              .`when`(ruleConfig.condition.ifEmpty { TRUE })

          ruleConfig.actions.forEach { customRule.then(it) }
          customRule
        }
        .toSet()
    return Rules(customRules)
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
     * fetches a list of facts of the given [relatedResourceKey] then iterates through this list in
     * order to return a list of all resources whose subject reference matches the logical Id of the
     * [resource]
     *
     * @param resource The parent resource for which the related resources will be retrieved
     * @param relatedResourceKey The key representing the relatedResources in the map
     * @param referenceFhirPathExpression A fhir path expression used to retrieve the subject
     * reference Id from the related resources
     */
    @Suppress("UNCHECKED_CAST")
    fun retrieveRelatedResources(
      resource: Resource,
      relatedResourceKey: String,
      referenceFhirPathExpression: String,
      relatedResourcesMap: Map<String, List<Resource>>? = null
    ): List<Resource> {
      val value: List<Resource> =
        relatedResourcesMap?.get(relatedResourceKey)
          ?: if (facts.getFact(relatedResourceKey) != null)
            facts.getFact(relatedResourceKey).value as List<Resource>
          else emptyList()

      return value.filter {
        resource.logicalId ==
          fhirPathDataExtractor.extractValue(it, referenceFhirPathExpression).extractLogicalIdUuid()
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
     *
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
          )
            label
          else null
        }
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
      label: String
    ): String? = mapResourcesToLabeledCSV(listOf(resource), fhirPathExpression, label)

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
    fun formatDate(
      inputDate: String,
      inputDateFormat: String,
      expectedFormat: String = "E, MMM dd yyyy"
    ): String? = inputDate.parseDate(inputDateFormat)?.formatDate(expectedFormat)

    /**
     * This function is responsible for formatting a date for whatever expectedFormat we need. It
     * takes an input a [date] as input and then it gives output in expected Format,
     * [expectedFormat] is by default (Example: Mon, Nov 5 2021)
     */
    fun formatDate(date: Date, expectedFormat: String = "E, MMM dd yyyy"): String =
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
     * This function filters resource provided the condition exracted from the [fhirPathExpression]
     * is met
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

    /** This function combines all string indexes to comma separated */
    fun joinToString(source: MutableList<String?>): String {
      source.removeIf { it == null }
      val inputString = source.joinToString()
      val regex = "(?<=^|,)[\\s,]*(\\w[\\w\\s]*)(?=[\\s,]*$|,)".toRegex()
      return regex.findAll(inputString).joinToString(", ") { it.groupValues[1] }
    }

    fun mapResourcesToExtractedValues(
      resources: List<Resource>?,
      fhirPathExpression: String
    ): List<Any> {
      if (fhirPathExpression.isEmpty()) {
        return emptyList()
      }
      return resources?.map { fhirPathDataExtractor.extractValue(it, fhirPathExpression) }
        ?: emptyList()
    }
  }

  companion object {
    private const val FHIR_PATH = "fhirPath"
    private const val DATA = "data"
    private const val TRUE = "true"
    private const val SERVICE = "service"
    private const val INCLUSIVE_SIX_DIGIT_MINIMUM = 100000
    private const val INCLUSIVE_SIX_DIGIT_MAXIMUM = 999999
  }
}
