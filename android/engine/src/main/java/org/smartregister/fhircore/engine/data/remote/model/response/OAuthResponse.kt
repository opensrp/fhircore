/*
 * Copyright 2021-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.engine.data.remote.model.response

import com.google.gson.annotations.SerializedName
import org.smartregister.fhircore.engine.util.annotation.ExcludeFromJacocoGeneratedReport

@ExcludeFromJacocoGeneratedReport
data class OAuthResponse(
  @SerializedName("access_token") var accessToken: String? = null,
  @SerializedName("token_type") var tokenType: String? = null,
  @SerializedName("refresh_token") var refreshToken: String? = null,
  @SerializedName("refresh_expires_in") var refreshExpiresIn: Int? = null,
  @SerializedName("expires_in") var expiresIn: Int? = null,
  @SerializedName("scope") var scope: String? = null
)
