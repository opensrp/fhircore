package org.smartregister.fhircore.quest

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.ui.core.inMilliseconds
import androidx.ui.core.minutes
import timber.log.Timber

class AppInActivityListener(val ignoreList: List<String>, onTimeLapse: () -> Unit) {
  val handler = Handler(Looper.getMainLooper())
  val runnable: Runnable = Runnable {
    if (ignoreList.firstOrNull { it == currentActivity?.name } == null)
      onTimeLapse()
  }
  var currentActivity: Class<Activity>? = null

  fun current(data: Class<Activity>) {
    currentActivity = data
  }

  fun start() {
    Timber.i("App is in background")
    handler.postDelayed(runnable, 5.minutes.inMilliseconds())
  }

  fun stop() {
    Timber.i("App is foreground")
    handler.removeCallbacks(runnable)
  }
}

interface OnInActivityListener {
  fun onTimeout()
}
