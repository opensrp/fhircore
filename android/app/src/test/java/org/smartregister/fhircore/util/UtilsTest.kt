package org.smartregister.fhircore.util

import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test

class UtilsTest {

    @Test
    fun getAgeFromDate_CalculatesAge(){
        Assert.assertEquals(1 , Utils.getAgeFromDate("2020-01-01", DateTime.parse("2021-01-01").toLocalDate()))
    }
}