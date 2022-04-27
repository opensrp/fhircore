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

package org.smartregister.fhircore.engine.util.helper

import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.PrimitiveType
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.utils.FHIRPathEngine
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.domain.model.RegisterData

object FhirMapperServices {
  fun parseMapping(
    config: String,
    registerData: RegisterData.RawRegisterData,
    configurationRegistry: ConfigurationRegistry,
    fhirPathEngine: FHIRPathEngine
  ): RegisterData {
    return configurationRegistry.retrieveRegisterDataMapperConfiguration(config)?.let {
      parseMapping(it, registerData, fhirPathEngine)
    }
      ?: registerData
  }

  fun parseMapping(
    mapperParam: Parameters.ParametersParameterComponent,
    registerData: RegisterData.RawRegisterData,
    fhirPathEngine: FHIRPathEngine
  ): RegisterData? {
    val contextData: MutableMap<String, Any> = registerData.details.toMutableMap()
    contextData[registerData.main.first] = registerData.main.second

    return parseMapping(mapperParam, contextData, registerData.main.second, fhirPathEngine)
  }

  fun parseMapping(
    mapperParam: Parameters.ParametersParameterComponent,
    contextData: MutableMap<String, Any>,
    base: Resource,
    fhirPathEngine: FHIRPathEngine
  ): RegisterData? {
    val className =
      mapperParam
        .extension
        .firstOrNull {
          it.url == "http://hl7.org/fhir/StructureDefinition/structuredefinition-explicit-type-name"
        }
        ?.value
        ?.toString()
    return className?.let {
      Class.forName(it).kotlin.constructors.first().let { construct ->
        val paramValues = mutableMapOf<KParameter, Any?>()
        construct.parameters.map { clsParam ->
          mapperParam
            .part
            .singleOrNull { it.name == clsParam.name }
            ?.let { partParam ->
              if (partParam.hasResource()) {
                if (clsParam.type.jvmErasure.isSubclassOf(Collection::class))
                  (contextData[partParam.name] as Collection<*>).map {
                    parseMapping(
                      (partParam.resource as Parameters).parameter.first(),
                      contextData,
                      it as Resource,
                      fhirPathEngine
                    )
                  }
                else
                  parseMapping(
                    (partParam.resource as Parameters).parameter.first(),
                    contextData,
                    contextData[partParam.name] as Resource,
                    fhirPathEngine
                  )
              } else
                partParam.value
                  ?.let {
                    fhirPathEngine.evaluate(
                      contextData,
                      null,
                      null,
                      base,
                      it.castToExpression(it).expression
                    )
                  }
                  ?.let {
                    if (clsParam.type.jvmErasure.isSubclassOf(Collection::class)) it
                    else
                      it.firstOrNull()?.let {
                        if (it.isPrimitive) (it as PrimitiveType<*>).value else it
                      }
                  }
            }
            ?.also { evalValue -> paramValues[clsParam] = evalValue }
        }

        construct.callBy(paramValues) as RegisterData
      }
    }
  }
}
