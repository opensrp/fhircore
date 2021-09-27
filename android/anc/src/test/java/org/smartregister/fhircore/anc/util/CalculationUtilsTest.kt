package org.smartregister.fhircore.anc.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationUtilsTest {

    @Test
    fun testBMI_viaStandardUnit() {
        val expectedBMI = 22.96
        val computedBMI = computeBMIViaStandardUnits(70.0, 160.0)
        assertEquals(expectedBMI, computedBMI, 0.1)
    }

    @Test
    fun testBMI_viaMetricUnit() {
        val expectedBMI = 22.90
        val computedBMI = computeBMIViaMetricUnits(1.78, 72.57)
        assertEquals(expectedBMI, computedBMI, 0.1)
    }
}