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

package org.smartregister.fhircore.geowidget.baselayers

import android.content.Context
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.Source
import com.mapbox.mapboxsdk.style.sources.VectorSource
import io.ona.kujaku.plugin.switcher.ExpressionArrayLiteral
import io.ona.kujaku.plugin.switcher.layer.BaseLayer
import io.ona.kujaku.utils.IOUtil
import io.ona.kujaku.utils.LayerUtil
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import timber.log.Timber

// todo - move this class to Kujaku project
class StreetSatelliteLayer(context: Context) : BaseLayer() {
  private val streetSourceId = "composite"
  private val sourcesList = ArrayList<Source>()
  private val layers = LinkedHashSet<Layer>()
  private val satelliteSourceId = "mapbox://mapbox.satellite"
  private val vectorSourceId = "mapbox://mapbox.mapbox-streets-v8"
  private val tileSize = 256

  init {
    createLayersAndSources(context)
  }

  protected fun createLayersAndSources(context: Context) {
    val rasterSource = RasterSource(satelliteSourceId, satelliteSourceId, tileSize)
    val streetSource = VectorSource(streetSourceId, vectorSourceId)
    sourcesList.add(streetSource)
    sourcesList.add(rasterSource)
    val layerUtil = LayerUtil()
    try {
      val jsonArray =
        JSONArray(
          IOUtil.readInputStreamAsString(context.assets.open("satellite_streets_style.json")),
        )
      for (i in 0 until jsonArray.length()) {
        val jsonObject = jsonArray.getJSONObject(i)
        val layer = layerUtil.getLayer(jsonObject.toString())
        if (layer != null && "hillshade" == layer.id && layer is FillLayer) {
          // Add the correct opacity
          val fillOpacityExpression =
            Expression.interpolate(
              Expression.Interpolator.linear(),
              Expression.zoom(),
              Expression.literal(14),
              Expression.match(
                Expression.get("level"),
                Expression.literal(0.12),
                Expression.stop(
                  ExpressionArrayLiteral(
                    arrayOf<Any>(67, 56),
                  ),
                  0.06,
                ),
                Expression.stop(
                  ExpressionArrayLiteral(
                    arrayOf<Any>(89, 78),
                  ),
                  0.05,
                ),
              ),
              Expression.literal(16),
              Expression.literal(0),
            )
          layer.withProperties(PropertyFactory.fillOpacity(fillOpacityExpression))
        }
        if (layer != null) {
          layers.add(layer)
        }
      }
    } catch (e: IOException) {
      Timber.e(e)
    } catch (e: JSONException) {
      Timber.e(e)
    }
  }

  override fun getDisplayName(): String {
    return "Satellite + Streets"
  }

  override fun getSourceIds(): Array<String> {
    return arrayOf(streetSourceId)
  }

  override fun getLayers(): LinkedHashSet<Layer> {
    return layers
  }

  override fun getSources(): List<Source> {
    return sourcesList
  }

  override fun getId(): String {
    return "satellite-street-base-layer"
  }

  override fun getLayerIds(): Array<String?> {
    val layerIds = arrayOfNulls<String>(layers.size)
    for ((counter, layer) in layers.withIndex()) {
      layerIds[counter] = layer.id
    }
    return layerIds
  }
}
