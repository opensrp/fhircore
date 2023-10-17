package org.smartregister.fhircore.engine.configuration.app

import kotlinx.serialization.Serializable

@Serializable
data class PermissionConfig(
    val name: String,
    val minSdkInt: Int = 0,
    val maxSdkInt: Int = 99,
) : java.io.Serializable
