package org.smartregister.fhircore.engine.performance

import timber.log.Timber

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 22-02-2023.
 */
class Timer(val startString: String = "", val methodName: String = "") {

    var startTime = 0L
    var stopTime = 0L

    init {
        startTime = System.currentTimeMillis()
        Timber.e(startString)
        Timber.e("Starting $methodName")
    }

    fun stop() {
        stopTime = System.currentTimeMillis()
        val timeTaken = stopTime - startTime
        val formattedTime = "%,d".format(timeTaken)
        Timber.e("Finished $methodName and it took $formattedTime ms / ${timeTaken/1000} secs")
    }

}