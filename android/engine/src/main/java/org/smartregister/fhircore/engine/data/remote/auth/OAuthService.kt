/*
 * Copyright 2021 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.data.remote.auth

import okhttp3.ResponseBody
import org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OAuthService {

  @FormUrlEncoded
  @POST("protocol/openid-connect/token")
  suspend fun fetchToken(@FieldMap(encoded = false) body: Map<String, String>): OAuthResponse

  @FormUrlEncoded
  @POST("protocol/openid-connect/logout")
  suspend fun logout(
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String,
    @Field("refresh_token") refreshToken: String
  ): Response<ResponseBody>
}
