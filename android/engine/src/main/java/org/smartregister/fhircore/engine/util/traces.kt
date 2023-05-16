package org.smartregister.fhircore.engine.util

import com.google.firebase.perf.FirebasePerformance.startTrace
import com.google.firebase.perf.metrics.Trace

inline fun <E> trace(name : String, block: (Trace) -> E): E {
    val trace = startTrace(name) //creates & starts a new Trace
    return try {
        block(trace)
    } finally {
        trace.stop()
    }
}