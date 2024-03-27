package org.smartregister.fhircore.geowidget.baselayers

import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.RasterLayer
import com.mapbox.mapboxsdk.style.sources.RasterSource
import com.mapbox.mapboxsdk.style.sources.Source
import io.ona.kujaku.plugin.switcher.layer.BaseLayer

//todo - move this class to Kujaku project
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

