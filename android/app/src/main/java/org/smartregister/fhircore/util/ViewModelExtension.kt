package org.smartregister.fhircore.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> T.createFactory(): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(this@createFactory::class.java)) {
                return this@createFactory as T
            }
            throw IllegalArgumentException("unexpected viewModel class $modelClass")
        }
    }
}