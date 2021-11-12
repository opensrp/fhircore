package org.smartregister.fhircore.engine.cql

import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.SimpleType
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import javax.xml.bind.JAXBContext
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Element
import org.cqframework.cql.elm.execution.Expression
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.TypeSpecifier
import org.opencds.cqf.cql.engine.elm.execution.ElementMixin
import org.opencds.cqf.cql.engine.elm.execution.ExpressionDefMixin
import org.opencds.cqf.cql.engine.elm.execution.ExpressionMixin
import org.opencds.cqf.cql.engine.elm.execution.LibraryWrapper
import org.opencds.cqf.cql.engine.elm.execution.TypeSpecifierMixin
import org.opencds.cqf.cql.engine.execution.CqlLibraryReader
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader
import timber.log.Timber

open class FhirLibraryLoader(
  modelManager: ModelManager,
  libraryContentProviders: List<LibraryContentProvider>,
  translatorOptions: CqlTranslatorOptions = CqlTranslatorOptions.defaultOptions(),
) : TranslatingLibraryLoader(modelManager, libraryContentProviders, translatorOptions) {

  val unmarshaller by lazy { jaxbContextV2.createUnmarshaller() }

  val jaxbContextV2 by lazy { JAXBContext.newInstance(org.hl7.elm.r1.Library::class.java) }

  override fun readXml(inputStream: InputStream): Library {
    return kotlin
      .runCatching { CqlLibraryReader.read(unmarshaller, inputStream) }
      .onFailure { Timber.e(it) }
      .getOrNull()!!
  }

  override fun translatorOptionsMatch(library: Library): Boolean {
    return true
  }

  override fun readJxson(inputStream: InputStream): Library {
    return kotlin
      .runCatching {
          read(InputStreamReader(inputStream))
          //JsonCqlLibraryReader.read(InputStreamReader(inputStream))
      }
      .onFailure { Timber.e(it) }
      .getOrNull()!!
  }

  fun read(reader: Reader): Library {
    val mapper = ObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    //mapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
    val module = JaxbAnnotationModule()
    mapper.registerModule(module)
    val simpleModule = SimpleModule()
    simpleModule.setMixInAnnotation(Element::class.java, ElementMixin::class.java)
    simpleModule.setMixInAnnotation(Expression::class.java, ExpressionMixin::class.java)
    simpleModule.setMixInAnnotation(TypeSpecifier::class.java, TypeSpecifierMixin::class.java)
   // simpleModule.setMixInAnnotation(ExpressionDef::class.java, ExpressionDefMixin::class.java);
    mapper.registerModule(simpleModule)
    mapper.addHandler(object: DeserializationProblemHandler() {
      override fun handleMissingTypeId(
        ctxt: DeserializationContext,
        baseType: JavaType,
        idResolver: TypeIdResolver,
        failureMsg: String?
      ): JavaType {
        val id = idResolver.idFromBaseType()
        val type = idResolver.typeFromId(ctxt, id)
        return if (baseType.isConcrete && type != null)
          type
        else
          super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg)
      }
    })
    return mapper.readValue(reader, LibraryWrapper::class.java).library
  }
}
