/*
 * Copyright 2021-2023 Ona Systems, Inc
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

import com.google.android.fhir.workflow.FhirOperator
import org.apache.commons.lang3.StringUtils
import org.cqframework.cql.cql2elm.LibrarySourceProvider
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Measure
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Parameters
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.execution.Context
import org.opencds.cqf.cql.engine.execution.LibraryLoader
import org.opencds.cqf.cql.engine.runtime.Interval
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal
import org.opencds.cqf.cql.evaluator.measure.common.MeasureEvalType
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureEvaluation
import org.opencds.cqf.cql.evaluator.measure.r4.R4MeasureProcessor
import org.smartregister.fhircore.engine.util.callSuperPrivateMember
import org.smartregister.fhircore.engine.util.getPrivateProperty

class R4MeasureProcessorExt(
  fhirEngineTerminologyProvider: TerminologyProvider,
  libraryContentProvider: LibrarySourceProvider,
  dataProvider: DataProvider,
  fhirEngineDal: FhirDal,
) :
  R4MeasureProcessor(
    fhirEngineTerminologyProvider,
    libraryContentProvider,
    dataProvider,
    fhirEngineDal,
  ) {
  override fun innerEvaluateMeasure(
    measure: Measure,
    periodStart: String?,
    periodEnd: String?,
    reportType: String?,
    subjectIds: List<String?>?,
    fhirDal: FhirDal,
    contentEndpoint: Endpoint?,
    terminologyEndpoint: Endpoint?,
    dataEndpoint: Endpoint?,
    additionalData: Bundle?,
  ): MeasureReport? {
    require(measure.hasLibrary()) {
      String.format(
        "Measure %s does not have a primary library specified",
        measure.url,
      )
    }
    val libraryUrl = measure.library[0]
    val libraries = fhirDal.searchByUrl("Library", libraryUrl.value)
    val libraryIter: Iterator<IBaseResource> = libraries.iterator()
    require(libraryIter.hasNext()) {
      String.format(
        "Unable to locate primary Library with url %s",
        libraryUrl.value,
      )
    }
    val primaryLibrary = libraryIter.next() as Library
    val librarySourceProvider =
      (if (contentEndpoint != null) {
        librarySourceProviderFactory.create(
          endpointConverter.getEndpointInfo(
            contentEndpoint,
          ),
        )
      } else {
        localLibrarySourceProvider
      })
        ?: throw IllegalStateException(
          "a librarySourceProvider was not provided and one could not be constructed",
        )
    val libraryLoader =
      callSuperPrivateMember<LibraryLoader>("buildLibraryLoader", librarySourceProvider)
    // buildLibraryLoader(librarySourceProvider)
    val library =
      libraryLoader.load(
        VersionedIdentifier().withId(primaryLibrary.name).withVersion(primaryLibrary.version),
      )
    val terminologyProvider =
      (if (terminologyEndpoint != null) {
        callSuperPrivateMember("buildTerminologyProvider", terminologyEndpoint)
      } else {
        localTerminologyProvider
      })
        ?: throw IllegalStateException(
          "a terminologyProvider was not provided and one could not be constructed",
        )
    val dataProvider =
      (if (
        dataEndpoint != null ||
          additionalData?.entry?.all { it.resource is Parameters }?.not() == true
      ) {
        callSuperPrivateMember(
          "buildDataProvider",
          dataEndpoint,
          additionalData,
          terminologyProvider,
        )
      } else {
        localDataProvider
      })
        ?: throw IllegalStateException(
          "a dataProvider was not provided and one could not be constructed",
        )
    var measurementPeriod: Interval? = null
    if (StringUtils.isNotBlank(periodStart) && StringUtils.isNotBlank(periodEnd)) {
      measurementPeriod = callSuperPrivateMember("buildMeasurementPeriod", periodStart, periodEnd)
    }

    val params =
      additionalData!!
        .entry
        .filter { it.resource is Parameters }
        .flatMap {
          val params = it.resource as Parameters
          params.parameter.map { it.name to (it.resource ?: it.value.primitiveValue()) }
        }
    val context =
      callSuperPrivateMember<Context>(
        "buildMeasureContext",
        library,
        libraryLoader,
        terminologyProvider,
        dataProvider,
      )

    params.forEach { context.setParameter(null, it.first, it.second) }
    val measureEvaluator = R4MeasureEvaluation(context, measure)
    return measureEvaluator.evaluate(
      MeasureEvalType.fromCode(reportType),
      subjectIds,
      measurementPeriod,
    )
  }

  fun evaluateMeasure(
    measureUrl: String,
    startDateFormatted: String,
    endDateFormatted: String,
    reportType: String,
    subject: String?,
    practitionerId: String?,
    params: Map<String, String>,
  ): MeasureReport {
    val additionalData =
      Bundle().apply {
        addEntry().resource =
          Parameters().apply { params.forEach { addParameter(it.key, it.value) } }
      }
    return evaluateMeasure(
      measureUrl,
      startDateFormatted,
      endDateFormatted,
      reportType,
      subject,
      practitionerId,
      null,
      null,
      null,
      null,
      additionalData,
    )
  }

  companion object {
    fun buildMeasureProcessorExt(fhirOperator: FhirOperator): R4MeasureProcessorExt {
      val fhirEngineTerminologyProvider =
        getPrivateProperty("fhirEngineTerminologyProvider", fhirOperator) as TerminologyProvider
      val librarySourceProvider =
        getPrivateProperty("libraryContentProvider", fhirOperator) as LibrarySourceProvider
      val compositeDataProvider =
        getPrivateProperty("dataProvider", fhirOperator) as CompositeDataProvider
      val fhirDal = getPrivateProperty("fhirEngineDal", fhirOperator) as FhirDal

      return R4MeasureProcessorExt(
        fhirEngineTerminologyProvider,
        librarySourceProvider,
        compositeDataProvider,
        fhirDal,
      )
    }
  }
}
