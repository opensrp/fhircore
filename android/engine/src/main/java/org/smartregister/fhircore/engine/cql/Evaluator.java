package org.smartregister.fhircore.engine.cql;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.cql2elm.CqlTranslatorOptions;
import org.cqframework.cql.cql2elm.ModelManager;
import org.cqframework.cql.elm.execution.Library;
import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.data.DataProvider;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver;
import org.opencds.cqf.cql.evaluator.CqlEvaluator;
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider;
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector;
import org.opencds.cqf.cql.evaluator.engine.execution.TranslatingLibraryLoader;
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory;
import org.opencds.cqf.cql.evaluator.library.CqlFhirParametersConverter;
import org.opencds.cqf.cql.evaluator.library.LibraryEvaluator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.api.BundleInclusionRule;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.BundleLinks;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;

/**
 * This class contains methods to run CQL evaluators given Fhir expressions
 * It borrows code from https://github.com/DBCG/CqlEvaluatorSampleApp
 * See also https://www.hl7.org/fhir/
 */
public class Evaluator {

    FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
    AdapterFactory adapterFactory = new AdapterFactory();
    FhirTypeConverter fhirTypeConverter;
    CqlFhirParametersConverter cqlFhirParametersConverter;
    LibraryVersionSelector libraryVersionSelector;
    LibraryContentProvider contentProvider;
    BundleTerminologyProvider terminologyProvider;
    BundleRetrieveProvider bundleRetrieveProvider;
    IParser parser;
    CqlEvaluator cqlEvaluator;
    LibraryEvaluator libraryEvaluator;

    /**
     * This method loads configurations for CQL evaluation
     * @param libraryData Fhir resource type Library
     * @param helperData Fhir resource type LibraryHelper
     * @param valueSetData Fhir resource type ValueSet
     * @param testData Fhir resource to evaluate e.g Patient
     */
    private void loadConfigs(String libraryData,
                               String helperData,
                               String valueSetData,
                               String testData) {

        this.parser = this.fhirContext.newJsonParser();
        this.parser.setPrettyPrint(true);

        this.libraryVersionSelector = new LibraryVersionSelector(this.adapterFactory);
        this.fhirTypeConverter = new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());
        this.cqlFhirParametersConverter = new CqlFhirParametersConverter(this.fhirContext, this.adapterFactory, this.fhirTypeConverter);
        // Load Library Content and create a LibraryContentProvider, which is the interface used by the LibraryLoader for getting library CQL/ELM/etc.
        InputStream libraryStream = new ByteArrayInputStream(libraryData.getBytes());
        InputStream fhirHelpersStream = new ByteArrayInputStream(helperData.getBytes());
        IBaseResource library = this.parser.parseResource(libraryStream);
        IBaseResource fhirHelpersLibrary = this.parser.parseResource(fhirHelpersStream);

        List<IBaseResource> resources = Lists.newArrayList(library, fhirHelpersLibrary);
        IVersionSpecificBundleFactory bundleFactory = this.fhirContext.newBundleFactory();

        BundleLinks bundleLinks = new BundleLinks("", null, true, BundleTypeEnum.COLLECTION);

        bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size(), null);

        bundleFactory.addResourcesToBundle(resources, BundleTypeEnum.COLLECTION, "",
                BundleInclusionRule.BASED_ON_INCLUDES, null);

        this.contentProvider = new BundleFhirLibraryContentProvider(this.fhirContext, (IBaseBundle) bundleFactory.getResourceBundle(), this.adapterFactory, this.libraryVersionSelector);

        // Load terminology content, and create a TerminologyProvider which is the interface used by the evaluator for resolving terminology
        InputStream valueSetStream = new ByteArrayInputStream(valueSetData.getBytes());
        IBaseResource valueSetBundle = this.parser.parseResource(valueSetStream);
        this.terminologyProvider = new BundleTerminologyProvider(this.fhirContext, (IBaseBundle) valueSetBundle);

        // Load data content, and create a RetrieveProvider which is the interface used for implementations of CQL retrieves.
        InputStream dataStream = new ByteArrayInputStream(testData.getBytes());
        IBaseResource dataBundle = this.parser.parseResource(dataStream);
        this.bundleRetrieveProvider = new BundleRetrieveProvider(this.fhirContext, (IBaseBundle) dataBundle);
        this.bundleRetrieveProvider.setTerminologyProvider(this.terminologyProvider);
        this.bundleRetrieveProvider.setExpandValueSets(true);

        this.cqlEvaluator = new CqlEvaluator(
                new TranslatingLibraryLoader(
                        new ModelManager(),
                        Collections.singletonList(this.contentProvider),
                        CqlTranslatorOptions.defaultOptions()) {

                    // This is a hack needed to circumvent a bug that's currently present in the cql-engine.
                    // By default, the LibraryLoader checks to ensure that the same translator options are used to for all libraries,
                    // And it will re-translate if possible. Since translating CQL is not currently possible
                    // on Android (some changes to the way ModelInfos are loaded is needed) the library loader just needs to load libraries
                    // regardless of whether the options match.
                    @Override
                    protected Boolean translatorOptionsMatch(Library library) {
                        return true;
                    }
                },
                new HashMap<String, DataProvider>() {{
                    put("http://hl7.org/fhir", new CompositeDataProvider(new R4FhirModelResolver(), bundleRetrieveProvider));
                }}, this.terminologyProvider);

        this.libraryEvaluator = new LibraryEvaluator(this.cqlFhirParametersConverter, cqlEvaluator);
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
    public String runCql(String libraryData,
                         String helperData,
                         String valueSetData,
                         String testData,
                         String evaluatorId,
                         String context,
                         String contextLabel) {

        loadConfigs(libraryData,
                helperData,
                valueSetData,
                testData);

        IBaseParameters result = libraryEvaluator.evaluate(
                new VersionedIdentifier().
                        withId(evaluatorId),
                Pair.of(context, contextLabel), null, null);
        return this.fhirContext.newJsonParser().encodeResourceToString(result);
    }
}
