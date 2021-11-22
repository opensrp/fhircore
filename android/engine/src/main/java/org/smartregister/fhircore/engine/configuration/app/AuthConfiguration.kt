package org.smartregister.fhircore.engine.configuration.app

/** Configurations for Keycloak server authentication loaded from the BuildConfig */
data class AuthConfiguration(
  var oauthServerBaseUrl: String,
  var fhirServerBaseUrl: String,
  var clientId: String,
  var clientSecret: String,
  var accountType: String,
  var scope: String = "openid"
)
