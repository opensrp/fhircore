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
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.evaluator.cql2elm.content.LibraryContentProvider
import org.opencds.cqf.cql.evaluator.cql2elm.content.fhir.BundleFhirLibraryContentProvider
import org.opencds.cqf.cql.evaluator.cql2elm.util.LibraryVersionSelector
import org.opencds.cqf.cql.evaluator.engine.model.CachingModelResolverDecorator
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider
import org.opencds.cqf.cql.evaluator.fhir.adapter.r4.AdapterFactory
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor

/**
 * This class contains methods to run Measure evaluations given Fhir expressions It borrows code
 * from https://github.com/DBCG/CqlEvaluatorSampleApp See also https://www.hl7.org/fhir/
 */
class MeasureEvaluator {

  var measureProcessor: R4MeasureProcessor? = null
  private val bundleLinks = BundleLinks("", null, true, BundleTypeEnum.COLLECTION)
  private val adapterFactory = AdapterFactory()
  private val libraryVersionSelector = LibraryVersionSelector(adapterFactory)

  /**
   * This method loads configurations for Measure evaluation
   * @param patientResources List of Fhir patient resources that are related
   * @param library Fhir resource type Library
   */
  private fun setup(
    patientResources: ArrayList<IBaseResource> = ArrayList(),
    library: IBaseBundle,
    fhirContext: FhirContext
  ) {

    val bundleFactory = fhirContext.newBundleFactory()!!
    bundleFactory.addRootPropertiesToBundle(
      "bundled-directory",
      bundleLinks,
      patientResources.size,
      null
    )
    bundleFactory.addResourcesToBundle(
      patientResources,
      BundleTypeEnum.COLLECTION,
      "",
      BundleInclusionRule.BASED_ON_INCLUDES,
      null
    )

    val libraryContentProvider: LibraryContentProvider =
      BundleFhirLibraryContentProvider(fhirContext, library, adapterFactory, libraryVersionSelector)
    val terminologyProvider: TerminologyProvider =
      BundleTerminologyProvider(
        fhirContext,
        library,
      )

    val bundleRetrieveProvider =
      BundleRetrieveProvider(fhirContext, bundleFactory.resourceBundle as IBaseBundle)

    bundleRetrieveProvider.terminologyProvider = terminologyProvider
    bundleRetrieveProvider.isExpandValueSets = true
    val dataProvider: DataProvider =
      CompositeDataProvider(
        CachingModelResolverDecorator(R4FhirModelResolver()),
        bundleRetrieveProvider
      )
    val fhirDal = BundleFhirDal(fhirContext, library)

    measureProcessor =
      R4MeasureProcessor(terminologyProvider, libraryContentProvider, dataProvider, fhirDal)
  }

  /**
   * This method is used to run measure evaluation
   * @param libraryData Fhir resource type Library
   * @param patientDataList List of Fhir patient resources that are related
   * @param url e.g. http://fhir.org/guides/who/anc-cds/Measure/ANCIND01
   * @param periodStartDate e.g. 2020-01-01
   * @param periodEndDate e.g. 2020-01-31
   * @param reportType e.g. subject
   * @param subject e.g. "patient-charity-otala-1
   */
  fun runMeasureEvaluate(
    patientResources: ArrayList<IBaseResource> = ArrayList(),
    library: IBaseBundle,
    fhirContext: FhirContext,
    url: String,
    periodStartDate: String,
    periodEndDate: String,
    reportType: String,
    subject: String,
  ): String {
    setup(patientResources, library, fhirContext)
    val result =
      measureProcessor?.evaluateMeasure(
        url,
        periodStartDate,
        periodEndDate,
        reportType,
        subject,
        null,
        null,
        null,
        null,
        null,
        null
      )

    return fhirContext.newJsonParser().encodeResourceToString(result)
  }
}
