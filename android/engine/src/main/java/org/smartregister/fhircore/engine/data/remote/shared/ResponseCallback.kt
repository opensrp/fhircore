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
