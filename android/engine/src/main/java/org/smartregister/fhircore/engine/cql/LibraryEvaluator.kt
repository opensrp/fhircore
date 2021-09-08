package org.smartregister.fhircore.engine.cql

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.context.api.BundleInclusionRule
import ca.uhn.fhir.model.valueset.BundleTypeEnum
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.api.BundleLinks
import com.google.common.collect.Lists
import org.apache.commons.lang3.tuple.Pair
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter
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
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

/**
 * This class contains methods to run CQL evaluators given Fhir expressions
 * It borrows code from https://github.com/DBCG/CqlEvaluatorSampleApp
 * See also https://www.hl7.org/fhir/
 */
class LibraryEvaluator {
    var fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    var adapterFactory = AdapterFactory()
    var fhirTypeConverter: FhirTypeConverter? = null
    var cqlFhirParametersConverter: CqlFhirParametersConverter? = null
    var libraryVersionSelector: LibraryVersionSelector? = null
    var contentProvider: LibraryContentProvider? = null
    var terminologyProvider: BundleTerminologyProvider? = null
    var bundleRetrieveProvider: BundleRetrieveProvider? = null
    var cqlEvaluator: CqlEvaluator? = null
    var libraryEvaluator: LibraryEvaluator? = null

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
        var parser: IParser? = null
        parser = fhirContext.newJsonParser()
        parser.setPrettyPrint(true)
        libraryVersionSelector = LibraryVersionSelector(adapterFactory)
        fhirTypeConverter = FhirTypeConverterFactory().create(fhirContext.version.version)
        cqlFhirParametersConverter =
            CqlFhirParametersConverter(fhirContext, adapterFactory, fhirTypeConverter)
        // Load Library Content and create a LibraryContentProvider, which is the interface used by the LibraryLoader for getting library CQL/ELM/etc.
        val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
        val fhirHelpersStream: InputStream = ByteArrayInputStream(helperData.toByteArray())
        val library = parser.parseResource(libraryStream)
        val fhirHelpersLibrary = parser.parseResource(fhirHelpersStream)
        val resources: List<IBaseResource> = Lists.newArrayList(library, fhirHelpersLibrary)
        val bundleFactory = fhirContext.newBundleFactory()
        val bundleLinks = BundleLinks("", null, true, BundleTypeEnum.COLLECTION)
        bundleFactory.addRootPropertiesToBundle(
            "bundled-directory",
            bundleLinks,
            resources.size,
            null
        )
        bundleFactory.addResourcesToBundle(
            resources, BundleTypeEnum.COLLECTION, "",
            BundleInclusionRule.BASED_ON_INCLUDES, null
        )
        contentProvider = BundleFhirLibraryContentProvider(
            fhirContext,
            bundleFactory.resourceBundle as IBaseBundle,
            adapterFactory,
            libraryVersionSelector
        )

        // Load terminology content, and create a TerminologyProvider which is the interface used by the evaluator for resolving terminology
        val valueSetStream: InputStream = ByteArrayInputStream(valueSetData.toByteArray())
        val valueSetBundle = parser.parseResource(valueSetStream)
        terminologyProvider = BundleTerminologyProvider(fhirContext, valueSetBundle as IBaseBundle)

        // Load data content, and create a RetrieveProvider which is the interface used for implementations of CQL retrieves.
        val dataStream: InputStream = ByteArrayInputStream(testData.toByteArray())
        val dataBundle = parser.parseResource(dataStream)
        bundleRetrieveProvider = BundleRetrieveProvider(fhirContext, dataBundle as IBaseBundle)
        bundleRetrieveProvider!!.terminologyProvider = terminologyProvider
        bundleRetrieveProvider!!.isExpandValueSets = true
        cqlEvaluator = CqlEvaluator(
            object : TranslatingLibraryLoader(
                ModelManager(), listOf(contentProvider),
                CqlTranslatorOptions.defaultOptions()
            ) { },
            object : HashMap<String?, DataProvider?>() {
                init {
                    put(
                        "http://hl7.org/fhir",
                        CompositeDataProvider(R4FhirModelResolver(), bundleRetrieveProvider)
                    )
                }
            }, terminologyProvider
        )
        libraryEvaluator = LibraryEvaluator(cqlFhirParametersConverter, cqlEvaluator)
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
        loadConfigs(
            libraryData,
            helperData,
            valueSetData,
            testData
        )
        val result = libraryEvaluator!!.evaluate(
            VersionedIdentifier().withId(evaluatorId),
            Pair.of(context, contextLabel), null, null
        )
        return fhirContext.newJsonParser().encodeResourceToString(result)
    }
}