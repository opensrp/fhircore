package org.smartregister.fhircore.engine.util.extension

fun <T> List<T>.safeSubList(indices: IntRange): List<T> =
        this.subList(indices.first, indices.last.coerceAtMost(this.size))