package org.smartregister.fhircore.api

import android.content.Context
import okhttp3.Interceptor
import org.smartregister.fhircore.auth.secure.SecureConfig
import timber.log.Timber

class OAuthInterceptor(context: Context) : Interceptor {
  private val mContext: Context = context

  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    Timber.i("Intercepted request for auth headers if needed")

    var request = chain.request()

    if (!request.url.pathSegments.containsAll(mutableListOf("protocol", "openid-connect", "token"))
    ) {
      val token = SecureConfig(mContext).retrieveSessionToken()
      if (token.isNullOrEmpty()) throw IllegalStateException("No session token found")

      Timber.i("Passing auth token for %s ,,,, %s", request.url.toString(), token)

      request = request.newBuilder().addHeader("Authorization", "Bearer $token").build()
    }

    return chain.proceed(request)
  }
}
