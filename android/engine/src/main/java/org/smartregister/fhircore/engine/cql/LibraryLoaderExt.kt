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

package org.smartregister.fhircore.engine.cql

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Element
import org.cqframework.cql.elm.execution.Expression
import org.cqframework.cql.elm.execution.Library
import org.opencds.cqf.cql.engine.elm.execution.ElementMixin
import org.opencds.cqf.cql.engine.elm.execution.ExpressionMixin
import org.opencds.cqf.cql.engine.elm.execution.LibraryWrapper
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader
import timber.log.Timber

open class LibraryLoaderExt(
  modelManager: ModelManager,
  libraryContentProviders: List<LibraryContentProvider>,
  translatorOptions: CqlTranslatorOptions = CqlTranslatorOptions.defaultOptions(),
) : TranslatingLibraryLoader(modelManager, libraryContentProviders, translatorOptions) {

  public override fun translatorOptionsMatch(library: Library): Boolean {
    return true
  }

  public override fun readJxson(inputStream: InputStream): Library {
    return kotlin
      .runCatching { read(InputStreamReader(inputStream)) }
      .onFailure { Timber.e(it) }
      .getOrNull()!!
  }

  fun read(reader: Reader): Library {
    val mapper = ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val module = JaxbAnnotationModule()
    mapper.registerModule(module)
    val simpleModule = SimpleModule()
    simpleModule.setMixInAnnotation(Element::class.java, ElementMixin::class.java)
    simpleModule.setMixInAnnotation(Expression::class.java, ExpressionMixin::class.java)
    mapper.registerModule(simpleModule)
    mapper.addHandler(
      object : DeserializationProblemHandler() {
        override fun handleMissingTypeId(
          ctxt: DeserializationContext,
          baseType: JavaType,
          idResolver: TypeIdResolver,
          failureMsg: String?
        ): JavaType {
          val id = idResolver.idFromBaseType()
          val type = idResolver.typeFromId(ctxt, id)
          return if (baseType.isConcrete && type != null) type
          else super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg)
        }
      }
    )
    return mapper.readValue(reader, LibraryWrapper::class.java).library
  }
}
