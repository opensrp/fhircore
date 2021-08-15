package org.smartregister.fhircore.engine.data.remote.shared

import retrofit2.Call
import retrofit2.Response

/**
 * Generic response handler used when [Response] is received from the callable site. Supports two
 * methods for handling response and failures]
 */
interface ResponseHandler<T> {
  /** Method to handle [response] from API [call] */
  fun handleResponse(call: Call<T>, response: Response<T>)

  /** Method to handle exception thrown from API [call] */
  fun handleFailure(call: Call<T>, throwable: Throwable)
}
