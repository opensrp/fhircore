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

import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.Source
import io.ona.kujaku.plugin.switcher.layer.BaseLayer

// todo - move this class to Kujaku project
class MapBoxSatelliteLayer : BaseLayer() {
  private val satelliteLayerId = "mapbox-satellite"
  private val satelliteSourceId = "mapbox://mapbox.satellite"
  private val layers: LinkedHashSet<Layer?> = LinkedHashSet()
  private val sources: MutableList<Source?> = ArrayList()

  init {
    createLayersAndSources()
  }

  private fun createLayersAndSources() {
    val rasterSource = RasterSource(satelliteSourceId, "mapbox://mapbox.satellite", 256)
    val rasterLayer = RasterLayer(satelliteLayerId, satelliteSourceId)
    rasterLayer.setSourceLayer("mapbox-satellite")
    layers.add(rasterLayer)
    sources.add(rasterSource)
  }

  override fun getDisplayName(): String {
    return "Mapbox Satellite"
  }

  override fun getSourceIds(): Array<String> {
    return arrayOf(satelliteSourceId)
  }

  override fun getLayers(): LinkedHashSet<Layer?> {
    return layers
  }

  override fun getSources(): MutableList<Source?> {
    return sources
  }

  override fun getId(): String {
    return "mapbox-satellite-base-layer"
  }

  override fun getLayerIds(): Array<String> {
    return arrayOf(satelliteLayerId)
  }
}
