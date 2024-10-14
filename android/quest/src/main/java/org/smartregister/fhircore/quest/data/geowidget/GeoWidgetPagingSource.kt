/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.data.geowidget

import android.database.SQLException
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.android.fhir.datacapture.extensions.logicalId
import kotlinx.serialization.json.JsonPrimitive
import org.hl7.fhir.r4.model.Location
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.extension.interpolate
import org.smartregister.fhircore.geowidget.model.GeoJsonFeature
import org.smartregister.fhircore.geowidget.model.Geometry
import timber.log.Timber

/** [RegisterRepository] function for loading data to the paging source. */
class GeoWidgetPagingSource(
  private val defaultRepository: DefaultRepository,
  private val resourceDataRulesExecutor: ResourceDataRulesExecutor,
  private val geoWidgetConfig: GeoWidgetConfiguration,
) : PagingSource<Int, GeoJsonFeature>() {

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GeoJsonFeature> {
    return try {
      val currentPage = params.key ?: 0
      val prevKey = if (currentPage > 0) currentPage - 1 else null

      val registerData =
        defaultRepository.searchResourcesRecursively(
          filterActiveResources = null,
          fhirResourceConfig = geoWidgetConfig.resourceConfig,
          configRules = null,
          secondaryResourceConfigs = null,
          filterByRelatedEntityLocationMetaTag =
            geoWidgetConfig.filterDataByRelatedEntityLocation == true,
          currentPage = currentPage,
          pageSize = DEFAULT_PAGE_SIZE,
        )

      val nextKey = if (registerData.isNotEmpty()) currentPage + 1 else null

      val data =
        registerData
          .asSequence()
          .filter { it.resource is Location }
          .filter { (it.resource as Location).hasPosition() }
          .filter { with((it.resource as Location).position) { hasLongitude() && hasLatitude() } }
          .map {
            Pair(
              it.resource as Location,
              resourceDataRulesExecutor.processResourceData(
                repositoryResourceData = it,
                ruleConfigs = geoWidgetConfig.servicePointConfig?.rules ?: emptyList(),
                params = emptyMap(),
              ),
            )
          }
          .map { (location, resourceData) ->
            GeoJsonFeature(
              id = location.logicalId,
              geometry =
                Geometry(
                  coordinates = // MapBox coordinates are represented as Long,Lat (NOT Lat,Long)
                  listOf(
                      location.position.longitude.toDouble(),
                      location.position.latitude.toDouble(),
                    ),
                ),
              properties =
                geoWidgetConfig.servicePointConfig?.servicePointProperties?.mapValues {
                  JsonPrimitive(it.value.interpolate(resourceData.computedValuesMap))
                } ?: emptyMap(),
            )
          }
          .toList()
      LoadResult.Page(data = data, prevKey = prevKey, nextKey = nextKey)
    } catch (exception: SQLException) {
      Timber.e(exception)
      LoadResult.Error(exception)
    } catch (exception: Exception) {
      Timber.e(exception)
      LoadResult.Error(exception)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, GeoJsonFeature>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }

  companion object {
    const val DEFAULT_PAGE_SIZE = 20
  }
}
