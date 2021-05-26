package org.smartregister.fhircore.robolectric.shadow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.sync.FhirDataSource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplication
import org.smartregister.fhircore.FhirApplication


@Implements(FhirApplication::class)
class FhirApplicationShadow : ShadowApplication() {

    private val dataSource =
        object : FhirDataSource {
            override suspend fun loadData(path: String): Bundle {
                return Bundle()
            }

            override suspend fun insert(
                resourceType: String,
                resourceId: String,
                payload: String
            ): Resource {
                return Patient()
            }

            override suspend fun update(
                resourceType: String,
                resourceId: String,
                payload: String
            ): OperationOutcome {
                return OperationOutcome()
            }

            override suspend fun delete(resourceType: String, resourceId: String): OperationOutcome {
                return OperationOutcome()
            }
        }

    @Implementation
    fun constructFhirEngine(): FhirEngine {
        val clazz = Class.forName("com.google.android.fhir.FhirServices\$Builder")
        val constructor = clazz.getDeclaredConstructor(FhirDataSource::class.java, Context::class.java)
        constructor.isAccessible = true
        val builder = constructor.newInstance(dataSource, ApplicationProvider.getApplicationContext())
        val obj = Class.forName("com.google.android.fhir.FhirServices").cast(
            builder.javaClass.getDeclaredMethod("build").invoke(builder)
        )
        val field = obj?.javaClass?.getDeclaredField("fhirEngine")
        field?.isAccessible = true
        return field?.get(obj) as FhirEngine
    }
}