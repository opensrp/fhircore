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

package org.smartregister.fhircore.engine.data.remote.shared

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A generic callback for any arbitrary [Response].Handling of response is delegated to the
 * [responseHandler]
 */
open class ResponseCallback<T>(private val responseHandler: ResponseHandler<T>) : Callback<T> {

  override fun onResponse(call: Call<T>, response: Response<T>) {
    responseHandler.handleResponse(call, response)
  }

  override fun onFailure(call: Call<T>, throwable: Throwable) {
    responseHandler.handleFailure(call, throwable)
  }
}
