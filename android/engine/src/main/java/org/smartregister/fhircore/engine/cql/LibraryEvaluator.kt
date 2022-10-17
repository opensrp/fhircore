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
import ca.uhn.fhir.context.api.BundleInclusionRule
import ca.uhn.fhir.model.valueset.BundleTypeEnum
import ca.uhn.fhir.rest.api.BundleLinks
import com.google.android.fhir.get
import com.google.android.fhir.logicalId
import com.google.common.collect.Lists
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.tuple.Pair
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import org.json.JSONObject
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.CqlEvaluator
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibrarySourceProvider
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.cql.evaluator.engine.CqlEngineOptions
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.util.extension.encodeResourceToString
import timber.log.Timber

/**
 * This class contains methods to run CQL evaluators given Fhir expressions It borrows code from
 * https://github.com/DBCG/CqlEvaluatorSampleApp See also https://www.hl7.org/fhir/
 */
@Singleton
class LibraryEvaluator @Inject constructor() {
  private val fhirContext = FhirContext.forR4Cached()
  private val parser = fhirContext.newJsonParser()
  private var adapterFactory = AdapterFactory()
  val bundleFactory = fhirContext.newBundleFactory()
  private var libraryVersionSelector = LibraryVersionSelector(adapterFactory)
  private var contentProvider: LibrarySourceProvider? = null
  private var terminologyProvider: BundleTerminologyProvider? = null
  private var bundleRetrieveProvider: BundleRetrieveProvider? = null
  private var cqlEvaluator: CqlEvaluator? = null
  private var libEvaluator: LibraryEvaluator? = null
  private val bundleLinks = BundleLinks("", null, true, BundleTypeEnum.COLLECTION)
  val fhirTypeConverter = FhirTypeConverterFactory().create(fhirContext.version.version)
  val cqlFhirParametersConverter =
    CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter)
  lateinit var fhirModelResolver: R4FhirModelResolverExt
  lateinit var modelManager: ModelManager
  var initialized = false

  fun initialize() {
    if (initialized) return

    fhirModelResolver = R4FhirModelResolverExt()
    modelManager = ModelManager()

    initialized = true
  }

  /**
   * This method loads configurations for CQL evaluation
   * @param libraryResources Fhir resource type Library
   * @param valueSetData Fhir resource type ValueSet
   * @param testData Fhir resource to evaluate e.g Patient
   */
  private fun loadConfigs(
    libraryResources: List<IBaseResource>,
    valueSetData: IBaseBundle,
    testData: IBaseBundle,
    fhirContext: FhirContext
  ) {
    bundleFactory.addRootPropertiesToBundle(
      "bundled-directory",
      bundleLinks,
      libraryResources.size,
      null
    )
    bundleFactory.addResourcesToBundle(
      libraryResources,
      BundleTypeEnum.COLLECTION,
      "",
      BundleInclusionRule.BASED_ON_INCLUDES,
      null
    )
    contentProvider =
      BundleFhirLibrarySourceProvider(
        fhirContext,
        bundleFactory.resourceBundle as IBaseBundle,
        adapterFactory,
        libraryVersionSelector
      )

    terminologyProvider = BundleTerminologyProvider(fhirContext, valueSetData)

    bundleRetrieveProvider = BundleRetrieveProvider(fhirContext, testData)
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
        terminologyProvider,
        CqlEngineOptions.defaultOptions().options
      )
    libEvaluator = LibraryEvaluator(cqlFhirParametersConverter, cqlEvaluator)
  }

  /**
   * This method is used to run a CQL Evaluation
   * @param valueSetData Fhir resource type ValueSet
   * @param testData Fhir resource to evaluate e.g Patient
   * @param evaluatorId Outcome Id of evaluation e.g ANCRecommendationA2
   * @param context Fhir context, eg. Patient
   * @param contextLabel Fhir context e.g. mom-with-anemia
   * @return JSON String Fhir Resource type Parameter
   */
  fun runCql(
    resources: List<IBaseResource>,
    valueSetData: IBaseBundle,
    testData: IBaseBundle,
    fhirContext: FhirContext,
    evaluatorId: String?,
    context: String,
    contextLabel: String
  ): String {
    loadConfigs(resources, valueSetData, testData, fhirContext)
    val result =
      libEvaluator!!.evaluate(
        VersionedIdentifier().withId(evaluatorId),
        Pair.of(context, contextLabel),
        null,
        null
      )
    return fhirContext.newJsonParser().encodeResourceToString(result)
  }

  /**
   * This method removes multiple patients in a bundle entry and is left with the first occurrence
   * and returns a bundle with patient entry
   * @param patientData
   */
  fun processCqlPatientBundle(patientData: String): String {
    val auxPatientDataObj = JSONObject(patientData)
    val oldJSONArrayEntry = auxPatientDataObj.getJSONArray("entry")
    val newJSONArrayEntry = JSONArray()
    for (i in 0 until oldJSONArrayEntry.length() - 1) {
      val resourceType =
        oldJSONArrayEntry.getJSONObject(i).getJSONObject("resource").getString("resourceType")
      if (i != 0 && !resourceType.equals("Patient")) {
        newJSONArrayEntry.put(oldJSONArrayEntry.getJSONObject(i))
      }
    }

    auxPatientDataObj.remove("entry")
    auxPatientDataObj.put("entry", newJSONArrayEntry)

    return auxPatientDataObj.toString()
  }

  suspend fun runCqlLibrary(
    libraryId: String,
    patient: Patient?,
    data: Bundle,
    // TODO refactor class by modular and single responsibility principle
    repository: DefaultRepository,
    outputLog: Boolean = false
  ): List<String> {
    initialize()

    val library = repository.fhirEngine.get<org.hl7.fhir.r4.model.Library>(libraryId)

    val helpers =
      library.relatedArtifact
        .filter { it.hasResource() && it.resource.startsWith("Library/") }
        .mapNotNull {
          repository.fhirEngine.get<org.hl7.fhir.r4.model.Library>(
            it.resource.replace("Library/", "")
          )
        }

    loadConfigs(
      library,
      helpers,
      Bundle(),
      // TODO check and handle when data bundle has multiple Patient resources
      createBundle(
        listOfNotNull(
          patient,
          *data.entry.map { it.resource }.toTypedArray(),
          *repository.search(library.dataRequirementFirstRep).toTypedArray()
        )
      )
    )

    val result =
      libEvaluator!!.evaluate(
        VersionedIdentifier().withId(library.name).withVersion(library.version),
        patient?.let { Pair.of("Patient", it.logicalId) },
        null,
        null
      ) as
        Parameters

    parser.setPrettyPrint(false)
    return result.parameter.mapNotNull { p ->
      (p.value ?: p.resource)?.let {
        Timber.d("Param found: ${p.name} with value: ${getStringRepresentation(it)}")

        if (p.name.equals(OUTPUT_PARAMETER_KEY) && it.isResource) {
          data.addEntry().apply { this.resource = p.resource }
          repository.create(it as Resource)
        }

        when {
          // send as result only if outlog needed or is an output param of primitive type
          outputLog -> "${p.name} -> ${getStringRepresentation(it)}"
          p.name.equals(OUTPUT_PARAMETER_KEY) && !it.isResource ->
            "${p.name} -> ${getStringRepresentation(it)}"
          else -> null
        }
      }
    }
  }

  fun getStringRepresentation(base: Base) =
    if (base.isResource) parser.encodeResourceToString(base as Resource) else base.toString()

  private fun loadConfigs(
    library: org.hl7.fhir.r4.model.Library,
    helpers: List<org.hl7.fhir.r4.model.Library>,
    valueSet: Bundle,
    data: Bundle,
  ) {
    parser.setPrettyPrint(true)
    // Load Library Content and create a LibraryContentProvider, which is the interface used by the
    // LibraryLoader for getting library CQL/ELM/etc.
    val resources: List<IBaseResource> = Lists.newArrayList(library, *helpers.toTypedArray())

    bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size, null)
    bundleFactory.addResourcesToBundle(
      resources,
      BundleTypeEnum.COLLECTION,
      "",
      BundleInclusionRule.BASED_ON_INCLUDES,
      null
    )

    val libraryProvider =
      BundleFhirLibrarySourceProvider(
        fhirContext,
        bundleFactory.resourceBundle as IBaseBundle,
        adapterFactory,
        libraryVersionSelector
      )

    // Load terminology content, and create a TerminologyProvider which is the interface used by the
    // evaluator for resolving terminology
    val terminologyProvider = BundleTerminologyProvider(fhirContext, valueSet)

    Timber.d("Cql with data: ${data.encodeResourceToString()}")

    // Load data content, and create a RetrieveProvider which is the interface used for
    // implementations of CQL retrieves.
    val retrieveProvider =
      BundleRetrieveProvider(fhirContext, data).apply {
        this.terminologyProvider = terminologyProvider
        this.isExpandValueSets = true
      }

    cqlEvaluator =
      CqlEvaluator(
        TranslatingLibraryLoader(
          modelManager,
          listOf(libraryProvider),
          CqlTranslatorOptions.defaultOptions()
        ),
        mapOf("http://hl7.org/fhir" to CompositeDataProvider(fhirModelResolver, retrieveProvider)),
        terminologyProvider,
        CqlEngineOptions.defaultOptions().options
      )

    libEvaluator = LibraryEvaluator(cqlFhirParametersConverter, cqlEvaluator)
  }

  fun createBundle(resources: List<Resource>): Bundle {
    val bundleFactory = fhirContext.newBundleFactory()
    bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size, null)
    bundleFactory.addResourcesToBundle(
      resources,
      BundleTypeEnum.COLLECTION,
      "",
      BundleInclusionRule.BASED_ON_INCLUDES,
      null
    )
    return bundleFactory.resourceBundle as Bundle
  }

  companion object {

    fun init() {
      GlobalScope.launch { LibraryEvaluator().initialize() }
    }

    const val OUTPUT_PARAMETER_KEY = "OUTPUT"
  }
}
