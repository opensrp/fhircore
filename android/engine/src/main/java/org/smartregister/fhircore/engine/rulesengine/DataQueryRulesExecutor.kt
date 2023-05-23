package org.smartregister.fhircore.engine.rulesengine

import javax.inject.Inject
import kotlin.system.measureTimeMillis
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.rulesengine.services.DateService
import org.smartregister.fhircore.engine.util.fhirpath.FhirPathDataExtractor
import timber.log.Timber

class DataQueryRulesExecutor @Inject constructor(val fhirPathDataExtractor: FhirPathDataExtractor) :
    RulesListener() {

  fun fireRules(rules: Rules): Map<String, Any> {
    facts =
        Facts().apply {
          put(FHIR_PATH, fhirPathDataExtractor)
          put(DATA, mutableMapOf<String, Any>())
          put(DATE_SERVICE, DateService)
        }
    if (BuildConfig.DEBUG) {
      val timeToFireRules = measureTimeMillis { rulesEngine.fire(rules, facts) }
      Timber.d("Rule executed in $timeToFireRules millisecond(s)")
    } else rulesEngine.fire(rules, facts)
    return facts.get(DATA) as Map<String, Any>
  }

  companion object {
    private const val DATE_SERVICE = "dateService"
  }
}
