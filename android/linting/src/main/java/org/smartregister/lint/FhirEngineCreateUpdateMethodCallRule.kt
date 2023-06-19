package org.smartregister.lint

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 14-06-2023.
 */
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class FhirEngineCreateUpdateMethodCallRule : Rule(
    ruleId = RuleId("$CUSTOM_RULE_SET_ID:fhir-engine-create-update-used"),
    about =
    About(
        maintainer = "OpenSRP Developers",
        repositoryUrl = "github.com/opensrp/fhircore"
    ),
) {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val filename = node.psi.containingFile.name

        // Ignore FhirEngine.create and FhirEngine.update call in tests. This should ideally not change
        val filesToSkip = arrayOf("DefaultRepository.kt", "ConfigurationRegistry.kt")

        val filePath = node.psi.containingFile.originalFile.virtualFile.path

        // Ignore FhirEngine.create and FhirEngine.update call in tests
        if (filePath.contains("test/java") || filePath.contains("androidTest/java")) {
            return
        }

        filesToSkip.forEach {
            if (filename.endsWith(it)) {
                return
            }
        }

        val parent = node.parent(ElementType.DOT_QUALIFIED_EXPRESSION)

        if (parent != null && node.elementType == ElementType.REFERENCE_EXPRESSION && node.text == FHIR_ENGINE_VARIABLE_NAME) {
            val children = parent.getChildren(null)

            // Example PSI Structure
            // fhirEngine.create(resource) -> [0]fhirEngine [1]. [2]create(resource)
            // fhirEngine.update(resource) -> [0]fhirEngine [1]. [2]update(resource)
            if (children.size > 1 && children[2].elementType == ElementType.CALL_EXPRESSION) {
                val funCall = children[2]

                if (funCall.text.startsWith("create")) {
                    emit(node.startOffset, CREATE_USE_ERROR_MSG, false)
                } else if (funCall.text.startsWith("update")) {
                    emit(node.startOffset, UPDATE_USE_ERROR_MSG, false)
                }
            }
        }
    }

    companion object {
        const val FHIR_ENGINE_VARIABLE_NAME = "fhirEngine"
        const val CREATE_USE_ERROR_MSG = "Do not use `FhirEngine.create` directly. Kindly use `DefaultRepository.create` or `DefaultRepository.addOrUpdate`"
        const val UPDATE_USE_ERROR_MSG = "Do not use `FhirEngine.update` directly. Kindly use `DefaultRepository.addOrUpdate` or `DefaultRepository.update`. `DefaultRepository.addOrUpdate` fills the resource with previous values where they are empty"
    }
}