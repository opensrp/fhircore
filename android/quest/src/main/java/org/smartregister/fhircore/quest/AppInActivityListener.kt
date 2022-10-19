package org.smartregister.fhircore.quest

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.ui.core.inMilliseconds
import androidx.ui.core.minutes
import timber.log.Timber

class AppInActivityListener(val ignoreList: List<String>, onTimeLapse: () -> Unit) {
  val handler = Handler(Looper.getMainLooper())
  val runnable: Runnable = Runnable { onTimeLapse() }

  fun start(data: Class<Activity>) {
    Timber.i("App is in background")
    val canStart = ignoreList.firstOrNull() { data.name == it }
    if (canStart == null) handler.postDelayed(runnable, 5.minutes.inMilliseconds())
  }

  fun stop(data: Class<Activity>) {
    Timber.i("App is foreground")
    val canStart = ignoreList.firstOrNull() { data.name == it }
    if (canStart == null) handler.removeCallbacks(runnable)
  }
}
