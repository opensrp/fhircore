package org.smartregister.fhircore.engine.util

import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TracingUtil @Inject constructor(val sharedPreferencesHelper: SharedPreferencesHelper){

    fun getUpperLimitDate(): Date {
       val savedDateMillis = sharedPreferencesHelper.read(KEY, -1L)
        val today = Date()
       return if (savedDateMillis == -1L) today else Date(savedDateMillis)
    }

    fun setUpperLimitDate(date: Date){
        sharedPreferencesHelper.write(KEY, date.time, async = true)
    }

    companion object{
        const val KEY = "simulated_today"
    }
}