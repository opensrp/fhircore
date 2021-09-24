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
import java.io.ByteArrayInputStream
import java.io.InputStream
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
import org.opencds.cqf.cql.evaluator.measure.MeasureEvalConfig
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor

class MeasureEvaluator {

  var fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
  var measureProcessor: R4MeasureProcessor? = null
  private var parser = fhirContext.newJsonParser()!!
  private val bundleFactory = fhirContext.newBundleFactory()!!
  private val bundleLinks = BundleLinks("", null, true, BundleTypeEnum.COLLECTION)
  val adapterFactory = AdapterFactory()
  val libraryVersionSelector = LibraryVersionSelector(adapterFactory)

  private fun setup(libraryData: String, patientDataList: List<String>) {
    val libraryStream: InputStream = ByteArrayInputStream(libraryData.toByteArray())
    val library = parser.parseResource(libraryStream) as IBaseBundle
    var resources: ArrayList<IBaseResource> = ArrayList()

    for (r in patientDataList) {
      val patientDataStream: InputStream = ByteArrayInputStream(r.toByteArray())
      val patientData = parser.parseResource(patientDataStream) as IBaseBundle
      resources.add(patientData)
    }

    bundleFactory.addRootPropertiesToBundle("bundled-directory", bundleLinks, resources.size, null)
    bundleFactory.addResourcesToBundle(
      resources,
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

    val config = MeasureEvalConfig.defaultConfig()
    measureProcessor =
      R4MeasureProcessor(
        null,
        null,
        null,
        null,
        null,
        terminologyProvider,
        libraryContentProvider,
        dataProvider,
        fhirDal,
        config
      )
  }

  fun runMeasureEvaluate(
    libraryData: String,
    patientDataList: List<String>,
    url: String,
    periodStartDate: String,
    periodEndDate: String,
    reportType: String,
    subject: String
  ): String {
    setup(libraryData, patientDataList)
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
