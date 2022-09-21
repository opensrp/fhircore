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

import com.google.android.fhir.logicalId
import java.util.ArrayList
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.apache.commons.jexl3.JexlException
import org.hl7.fhir.r4.model.Resource
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.jeasy.rules.jexl.JexlRule
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RuleConfig
import org.smartregister.fhircore.engine.util.extension.translationPropertyKey
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import org.smartregister.fhircore.engine.util.helper.LocalizationHelper
import timber.log.Timber

@Singleton
class RulesFactory
@Inject
constructor(
  val configurationRegistry: ConfigurationRegistry,
  val fhirPathDataExtractor: FhirPathDataExtractor
) : RuleListener {

  private var facts: Facts = Facts()
  private val rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
  private val computedValuesMap = mutableMapOf<String, Any>()
  val rulesEngineService = RulesEngineService()

  init {
    rulesEngine.registerRuleListener(this)
    facts.apply {
      put(FHIR_PATH, fhirPathDataExtractor)
      put(DATA, computedValuesMap)
      put(SERVICE, rulesEngineService)
    }
  }

  override fun beforeEvaluate(rule: Rule, facts: Facts): Boolean = true

  override fun onSuccess(rule: Rule, facts: Facts) {
    if (BuildConfig.DEBUG) {
      Timber.d("Rule executed: %s -> %s", rule, computedValuesMap[rule.name])
    }
  }

  override fun onFailure(rule: Rule, facts: Facts, exception: Exception?) =
    when (exception) {
      is JexlException -> Timber.e(exception.cause)
      else -> Timber.e(exception)
    }

  override fun beforeExecute(rule: Rule, facts: Facts) = Unit

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
    // Reset previously computed values first
    computedValuesMap.clear()

    val customRules = mutableSetOf<Rule>()
    ruleConfigs.forEach { ruleConfig ->

      // Create JEXL rule
      val customRule: JexlRule =
        JexlRule()
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
      fhirPathExpression: String
    ): List<Resource> {
      if (facts.getFact(relatedResourceType) == null) return emptyList()
      val value = facts.getFact(relatedResourceType).value as ArrayList<Resource>

      return value.filter {
        resource.logicalId ==
          fhirPathDataExtractor
            .extractValue(it, fhirPathExpression)
            .substringAfterLast(delimiter = '/', missingDelimiterValue = "")
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
    fun retrieveParentResource(
      childResource: Resource,
      parentResourceType: String,
      fhirPathExpression: String
    ): Resource? {
      val value = facts.getFact(parentResourceType).value as ArrayList<Resource>
      val parentResourceId =
        fhirPathDataExtractor
          .extractValue(childResource, fhirPathExpression)
          .substringAfterLast(delimiter = '/', missingDelimiterValue = "")

      return value.find { it.logicalId == parentResourceId }
    }
  }

  companion object {
    private const val FHIR_PATH = "fhirPath"
    private const val DATA = "data"
    private const val TRUE = "true"
    private const val SERVICE = "service"
  }
}
