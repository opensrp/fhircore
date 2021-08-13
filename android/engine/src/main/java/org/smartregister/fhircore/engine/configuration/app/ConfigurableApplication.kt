package org.smartregister.fhircore.engine.configuration.app

/**
 * An interface that provides the application configurations. Every FHIR based application is
 * required to implement this interface and call the [configureApplication] method on the OnCreate
 * method of the Application class. Other classes will then be able to access the global e.g. the
 * serverUrl [ApplicationConfiguration]
 */
interface ConfigurableApplication {

  val applicationConfiguration: ApplicationConfiguration

  fun configureApplication(applicationConfiguration: ApplicationConfiguration)
}
