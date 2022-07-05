package org.smartregister.fhircore.engine.rulesengine

import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rule
import org.jeasy.rules.api.RuleListener
import org.jeasy.rules.api.Rules
import org.jeasy.rules.core.DefaultRulesEngine
import org.mvel2.CompileException
import timber.log.Timber
import java.util.HashSet

class RulesFactory: RuleListener {

    private var facts: Facts = Facts()
    private var rulesEngine: DefaultRulesEngine = DefaultRulesEngine()
    private var executableRulesList: HashSet<Rule> = hashSetOf()

        init {
        rulesEngine.registerRuleListener(this)
    }

    override fun beforeEvaluate(rule: Rule?, facts: Facts?): Boolean = true

    override fun onSuccess(rule: Rule?, facts: Facts?) = Timber.d("%s executed successfully", rule)

    override fun onFailure(rule: Rule?, facts: Facts?, exception: Exception?) =
        when (exception) {
            is CompileException -> Timber.e(exception.cause)
            else -> Timber.e(exception)
        }

    override fun beforeExecute(rule: Rule?, facts: Facts?) = Unit

    override fun afterEvaluate(rule: Rule?, facts: Facts?, evaluationResult: Boolean) = Unit

    fun updateFactsAndExecuteRules() {
       fireRules()
    }

    fun readRulesFromFile() {
        //TODO Implement this
    }

    private fun fireRules() {
        rulesEngine.fire(Rules(executableRulesList), facts)
    }

}