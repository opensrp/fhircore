package org.smartregister.fhircore.engine.util.extension
import androidx.lifecycle.MutableLiveData
import java.util.LinkedList
/*
operator fun <T> MutableLiveData<LinkedList<T>>.plusAssign(item: T) {
    val value = this.value ?: LinkedList<T>()
    value.add(item)
    this.value = value
}*/

// for immutable list
operator fun <T> MutableLiveData<List<T>>.plusAssign(item: T) {
    val value = this.value ?: emptyList()
    this.value = value + listOf(item)
}