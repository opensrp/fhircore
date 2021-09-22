package org.smartregister.fhircore.engine.cql

import android.content.Context
import org.smartregister.fhircore.engine.cql.CQLAssetBundler.getContentBundle
import org.smartregister.fhircore.engine.cql.CQLAssetBundler.getDataBundle
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider
import org.smartregister.fhircore.engine.cql.CQLAssetBundler
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal
import org.opencds.cqf.cql.evaluator.builder.EndpointConverter
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.measure.MeasureEvalConfig
import java.io.ByteArrayInputStream
import java.io.InputStream

class MeasureEvaluator {
    var fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    var measureProcessor: R4MeasureProcessor? = null
    var adapterFactory = AdapterFactory()
    var libraryVersionSelector = LibraryVersionSelector(adapterFactory)
    private var parser = fhirContext.newJsonParser()!!

    private fun setup(context: Context,libraryData: String) {
        parser.setPrettyPrint(true)
        val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
        val library = parser.parseResource(libraryStream) as IBaseBundle

        val libraryContentProvider: LibraryContentProvider = BundleFhirLibraryContentProvider(
            fhirContext,
            library,
            adapterFactory, libraryVersionSelector
        )



        val terminologyProvider: TerminologyProvider = BundleTerminologyProvider(
            fhirContext,
            getContentBundle(fhirContext, context.assets)
        )
        val bundleRetrieveProvider = BundleRetrieveProvider(
            fhirContext,
            getDataBundle(fhirContext, context.assets)
        )

        bundleRetrieveProvider.terminologyProvider = terminologyProvider
        bundleRetrieveProvider.isExpandValueSets = true

        val dataProvider: DataProvider = CompositeDataProvider(
            CachingModelResolverDecorator(
                R4FhirModelResolver()
            ), bundleRetrieveProvider
        )
        val fhirDal = BundleFhirDal(
            fhirContext,
            getContentBundle(fhirContext, context.assets)
        )
        val endpointConverter = EndpointConverter(adapterFactory)
        val config = MeasureEvalConfig.defaultConfig()
        measureProcessor = R4MeasureProcessor(
            null, null,
            null, null, null,
            terminologyProvider, libraryContentProvider, dataProvider, fhirDal, config
        )
    }

    fun runMeasureEvaluate(context: Context): String {
        setup(context)
        val result = measureProcessor
            ?.evaluateMeasure(
                "http://fhir.org/guides/who/anc-cds/Measure/ANCIND01",
                "2020-01-01",
                "2020-01-31",
                "subject",
                "patient-charity-otala-1",
                null, null,
                null, null,
                null, null
            )
        return fhirContext.newJsonParser().encodeResourceToString(result)
    }
}