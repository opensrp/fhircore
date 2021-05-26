package org.smartregister.fhircore.robolectric.activity

import android.app.Activity
import org.junit.After
import org.smartregister.fhircore.robolectric.RobolectricTest

abstract class ActivityRobolectricTest : RobolectricTest() {

    @After
    fun testDown() {
        getActivity().finish()
    }

    abstract fun getActivity(): Activity
}