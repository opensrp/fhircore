package org.smartregister.fhircore.engine.configuration

enum class AppConfigClassification : ConfigClassification {
    APPLICATION,
    LOGIN;
    override val classification: String = name.lowercase()
}