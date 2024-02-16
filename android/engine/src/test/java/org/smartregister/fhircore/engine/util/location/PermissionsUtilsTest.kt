package org.smartregister.fhircore.engine.util.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PermissionUtilsTest {

    val context = mockk<Context>()

    @Test
    fun checkAllPermissionsGranted(){
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

        every { ContextCompat.checkSelfPermission(context, any())
        } returns PackageManager.PERMISSION_GRANTED

        val result = PermissionUtils.checkPermissions(context, permissions)

        assertTrue(result)
    }

    @Test
    fun `checkPermissions should return false when any permission is not granted`(){
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

        every { ContextCompat.checkSelfPermission(context, any())
        } returns PackageManager.PERMISSION_DENIED

        val result = PermissionUtils.checkPermissions(context, permissions)

        assertFalse(result)
    }


}
