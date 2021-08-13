package org.smartregister.fhircore.engine.configuration.view

import androidx.compose.runtime.Stable
import org.smartregister.fhircore.engine.configuration.Configuration

@Stable
class LoginViewConfiguration(
  val showPowered: Boolean = true,
  val applicationName: String = "App Name",
  val applicationVersion: String = "0.0.1"
) : Configuration
/**
 * A function providing a DSL for configuring [LoginViewConfiguration]. The configurations provided
 * by this method are used on the register calling this method
 * @param showPowered Show or hide the powered by section
 * @param applicationName Set the application name
 * @param applicationVersion Set the application version
 */
@Stable
fun loginViewConfigurationOf(
  showPowered: Boolean = true,
  applicationName: String = "App Name",
  applicationVersion: String = "0.0.1"
) =
  LoginViewConfiguration(
    showPowered = showPowered,
    applicationName = applicationName,
    applicationVersion = applicationVersion
  )
