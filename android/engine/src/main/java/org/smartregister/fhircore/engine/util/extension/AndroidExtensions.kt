package org.smartregister.fhircore.engine.util.extension

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, toastLength: Int = Toast.LENGTH_LONG) =
  Toast.makeText(this, message, toastLength).show()
