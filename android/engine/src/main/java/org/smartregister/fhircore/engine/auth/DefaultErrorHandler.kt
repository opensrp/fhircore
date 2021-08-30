package org.smartregister.fhircore.engine.auth

import android.os.Handler
import android.os.Message
import timber.log.Timber

/** Subclass of [Handler.Callback] that logs the error message to the console */
object DefaultErrorHandler : Handler.Callback {
  override fun handleMessage(msg: Message): Boolean {
    Timber.i("Encountered an error while retrieving token: ", msg)
    return true
  }
}
