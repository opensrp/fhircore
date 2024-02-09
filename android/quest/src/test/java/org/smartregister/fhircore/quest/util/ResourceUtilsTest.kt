package org.smartregister.fhircore.quest.util

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ResourceUtilsTest {

    @Test
    fun testLocationResourceIsCreated() {
        val location: Location = mockk()
        every { location.longitude } returns 10.0
        every { location.latitude } returns 20.0
        every { location.altitude } returns 30.0

        val locationResource = ResourceUtils.createLocationResource(location)

        assertNotNull(locationResource.id)
        assertEquals(locationResource.position.longitude.toDouble(), 10.0)
        assertEquals(locationResource.position.latitude.toDouble(), 20.0)
        assertEquals(locationResource.position.altitude.toDouble(), 30.0)

    }


}