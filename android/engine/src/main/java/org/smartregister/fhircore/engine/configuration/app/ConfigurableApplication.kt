package org.smartregister.fhircore.engine.configuration.app

import com.google.android.fhir.FhirEngine
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.auth.AuthenticationService
import org.smartregister.fhircore.engine.util.SecureSharedPreference

/**
 * An interface that provides the application configurations. Every FHIR based application is
 * required to implement this interface and call the [configureApplication] method on the OnCreate
 * method of the Application class. Other classes will then be able to access the global e.g. the
 * serverUrl [ApplicationConfiguration]
 *
 * @property applicationConfiguration Set application configurations
 * @property authenticationService Set singleton instance of [AuthenticationService] used for
 * authenticating users
 * @property fhirEngine Set [FhirEngine]
 * @property secureSharedPreference Set singleton of [SecureSharedPreference] used to access
 * encrypted shared preference data
 * @property resourceSyncParams Set [FhirEngine] resource sync params needed for syncing data from
 * the server
 */
interface ConfigurableApplication {

  val applicationConfiguration: ApplicationConfiguration

  val authenticationService: AuthenticationService

  val fhirEngine: FhirEngine

  val secureSharedPreference: SecureSharedPreference

  val resourceSyncParams: Map<ResourceType, Map<String, String>>

  /** Provide [applicationConfiguration] for the Application */
  fun configureApplication(applicationConfiguration: ApplicationConfiguration)
}
