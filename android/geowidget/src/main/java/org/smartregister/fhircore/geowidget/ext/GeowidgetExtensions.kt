package org.smartregister.fhircore.geowidget.ext

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 16-08-2022.
 */

typealias Coordinate = Pair<Double, Double>

val Coordinate.latitude : Double
    get() = this.second

val Coordinate.longitude: Double
    get() = this.first