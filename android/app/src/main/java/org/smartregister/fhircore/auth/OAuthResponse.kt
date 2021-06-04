package org.smartregister.fhircore.auth

import com.google.gson.annotations.SerializedName

class OAuthResponse {
  @SerializedName("access_token") var accessToken: String? = null

  @SerializedName("token_type") var tokenType: String? = null

  @SerializedName("refresh_token") var refreshToken: String? = null

  @SerializedName("refresh_expires_in") var refreshExpiresIn: Int? = null

  @SerializedName("expires_in") var expiresIn: Int? = null

  @SerializedName("scope") var scope: String? = null
}
