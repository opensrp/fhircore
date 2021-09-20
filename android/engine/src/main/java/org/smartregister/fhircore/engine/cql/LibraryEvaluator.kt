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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.api.BundleInclusionRule
import ca.uhn.fhir.model.valueset.BundleTypeEnum
import ca.uhn.fhir.rest.api.BundleLinks
import com.google.common.collect.Lists
import java.io.ByteArrayInputStream
import java.io.InputStream
import org.apache.commons.lang3.tuple.Pair
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.CqlEvaluator
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator

/**
 * This class contains methods to run CQL evaluators given Fhir expressions It borrows code from
 * https://github.com/DBCG/CqlEvaluatorSampleApp See also https://www.hl7.org/fhir/
 */
class LibraryEvaluator {
  private var fhirContext = FhirContext.forCached(FhirVersionEnum.R4)!!
  private var adapterFactory = AdapterFactory()
  private var fhirTypeConverter = FhirTypeConverterFactory().create(fhirContext.version.version)
  private var cqlFhirParametersConverter =
    CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter)
  private var libraryVersionSelector = LibraryVersionSelector(adapterFactory)
  private var contentProvider: LibraryContentProvider? = null
  private var terminologyProvider: BundleTerminologyProvider? = null
  private var bundleRetrieveProvider: BundleRetrieveProvider? = null
  private var cqlEvaluator: CqlEvaluator? = null
  private var libEvaluator: LibraryEvaluator? = null
  private var parser = fhirContext.newJsonParser()!!
  private val bundleFactory = fhirContext.newBundleFactory()!!
  private val bundleLinks = BundleLinks("", null, true, BundleTypeEnum.COLLECTION)

  /**
   * This method loads configurations for CQL evaluation
   * @param libraryData Fhir resource type Library
   * @param helperData Fhir resource type LibraryHelper
   * @param valueSetData Fhir resource type ValueSet
   * @param testData Fhir resource to evaluate e.g Patient
   */
  private fun loadConfigs(
    libraryData: String,
    helperData: String,
    valueSetData: String,
    testData: String
  ) {
    parser.setPrettyPrint(true)
    // Load Library Content and create a LibraryContentProvider, which is the interface used by the
    // LibraryLoader for getting library CQL/ELM/etc.
    val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
    val fhirHelpersStream: InputStream = ByteArrayInputStream(helperData.toByteArray())
    val library = parser.parseResource(libraryStream)
    val fhirHelpersLibrary = parser.parseResource(fhirHelpersStream)
    val resources: List<IBaseResource> = Lists.newArrayList(library, fhirHelpersLibrary)
    bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size, null)
    bundleFactory.addResourcesToBundle(
      resources,
      BundleTypeEnum.COLLECTION,
      "",
      BundleInclusionRule.BASED_ON_INCLUDES,
      null
    )
    contentProvider =
      BundleFhirLibraryContentProvider(
        fhirContext,
        bundleFactory.resourceBundle as IBaseBundle,
        adapterFactory,
        libraryVersionSelector
      )

    // Load terminology content, and create a TerminologyProvider which is the interface used by the
    // evaluator for resolving terminology
    val valueSetStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
    val valueSetBundle = parser.parseResource(valueSetStream)
    terminologyProvider = BundleTerminologyProvider(fhirContext, valueSetBundle as IBaseBundle)

    // Load data content, and create a RetrieveProvider which is the interface used for
    // implementations of CQL retrieves.
    val dataStream: InputStream = ByteArrayInputStream(testData.toByteArray())
    val dataBundle = parser.parseResource(dataStream)
    bundleRetrieveProvider = BundleRetrieveProvider(fhirContext, dataBundle as IBaseBundle)
    bundleRetrieveProvider!!.terminologyProvider = terminologyProvider
    bundleRetrieveProvider!!.isExpandValueSets = true
    cqlEvaluator =
      CqlEvaluator(
        object :
          TranslatingLibraryLoader(
            ModelManager(),
            listOf(contentProvider),
            CqlTranslatorOptions.defaultOptions()
          ) {
          // This is a hack needed to circumvent a bug that's currently present in the cql-engine.
          // By default, the LibraryLoader checks to ensure that the same translator options are
          // used to for all libraries,
          // And it will re-translate if possible. Since translating CQL is not currently possible
          // on Android (some changes to the way ModelInfos are loaded is needed) the library loader
          // just needs to load libraries
          // regardless of whether the options match.
          override fun translatorOptionsMatch(library: Library): Boolean {
            return true
          }
        },
        object : HashMap<String?, DataProvider?>() {
          init {
            put(
              "http://hl7.org/fhir",
              CompositeDataProvider(R4FhirModelResolver(), bundleRetrieveProvider)
            )
          }
        },
        terminologyProvider
      )
    libEvaluator = LibraryEvaluator(cqlFhirParametersConverter, cqlEvaluator)
  }

  /**
   * This method is used to run a CQL Evaluation
   * @param libraryData Fhir resource type Library
   * @param helperData Fhir resource type LibraryHelper
   * @param valueSetData Fhir resource type ValueSet
   * @param testData Fhir resource to evaluate e.g Patient
   * @param evaluatorId Outcome Id of evaluation e.g ANCRecommendationA2
   * @param context Fhir context, eg. Patient
   * @param contextLabel Fhir context e.g. mom-with-anemia
   * @return JSON String Fhir Resource type Parameter
   */
  fun runCql(
    libraryData: String,
    helperData: String,
    valueSetData: String,
    testData: String,
    evaluatorId: String?,
    context: String,
    contextLabel: String
  ): String {
    loadConfigs(libraryData, helperData, valueSetData, testData)
    val result =
      libEvaluator!!.evaluate(
        VersionedIdentifier().withId(evaluatorId),
        Pair.of(context, contextLabel),
        null,
        null
      )
    return fhirContext.newJsonParser().encodeResourceToString(result)
  }
}
