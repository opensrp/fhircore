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

package org.smartregister.lint

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class FhirEngineCreateUpdateMethodCallRule :
  Rule(
    ruleId = RuleId("$CUSTOM_RULE_SET_ID:fhir-engine-create-update-used"),
    about = About(maintainer = "OpenSRP Developers", repositoryUrl = "github.com/opensrp/fhircore"),
  ) {

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
  ) {
    // Ignore FhirEngine.create and FhirEngine.update call in tests. This should ideally not change
    val filesToSkip = arrayOf("DefaultRepository.kt", "ConfigurationRegistry.kt")
    val filePath = node.psi.containingFile.originalFile.virtualFile.path

    // Ignore FhirEngine.create and FhirEngine.update call in tests
    if (filePath.contains("test/java") || filePath.contains("androidTest/java")) {
      return
    }

    val filename = node.psi.containingFile.name
    filesToSkip.forEach {
      if (filename.endsWith(it)) {
        return
      }
    }

    val parent = node.parent(ElementType.DOT_QUALIFIED_EXPRESSION)

    if (
      parent != null &&
        node.elementType == ElementType.REFERENCE_EXPRESSION &&
        node.text == FHIR_ENGINE_VARIABLE_NAME
    ) {
      val children = parent.getChildren(null)

      // Example PSI Structure
      // fhirEngine.create(resource) -> [0]fhirEngine [1]. [2]create(resource)
      // fhirEngine.update(resource) -> [0]fhirEngine [1]. [2]update(resource)
      if (hasFunctionCall(children)) {
        val functionCallNode = getFunctionCallNode(children)

        if (functionCallNode.text.startsWith("create")) {
          emit(node.startOffset, CREATE_USE_ERROR_MSG, false)
        } else if (functionCallNode.text.startsWith("update")) {
          emit(node.startOffset, UPDATE_USE_ERROR_MSG, false)
        }
      }
    }
  }

  private fun hasFunctionCall(children: Array<out ASTNode>): Boolean {
    return children.size > 1 && children[2].elementType == ElementType.CALL_EXPRESSION
  }

  private fun getFunctionCallNode(children: Array<out ASTNode>) = children[2]

  companion object {
    const val FHIR_ENGINE_VARIABLE_NAME = "fhirEngine"
    const val CREATE_USE_ERROR_MSG =
      "Do not use `FhirEngine.create` directly. Kindly use `DefaultRepository.create` or `DefaultRepository.addOrUpdate`"
    const val UPDATE_USE_ERROR_MSG =
      "Do not use `FhirEngine.update` directly. Kindly use `DefaultRepository.addOrUpdate` or `DefaultRepository.update`. `DefaultRepository.addOrUpdate` fills the resource with previous values where they are empty"
  }
}
