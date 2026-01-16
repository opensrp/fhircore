/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri.parse
import android.widget.Toast
import org.smartregister.fhircore.engine.configuration.ExternalAppConfig
import org.smartregister.fhircore.quest.R

fun openExternalApp(context: Context, config: ExternalAppConfig) {
  if (config.packageName.isBlank()) {
    Toast.makeText(
        context,
        context.getString(R.string.external_app_invalid_config),
        Toast.LENGTH_SHORT,
      )
      .show()
    return
  }

  val launchIntent = context.packageManager.getLaunchIntentForPackage(config.packageName)
  if (launchIntent != null) {
    context.startActivity(launchIntent)
  } else {
    if (!config.playStoreUrl.isNullOrBlank()) {
      try {
        val browserIntent = Intent(Intent.ACTION_VIEW, parse(config.playStoreUrl))
        context.startActivity(browserIntent)
      } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.external_app_not_installed),
            Toast.LENGTH_SHORT,
          )
          .show()
      }
    } else {
      Toast.makeText(
          context,
          context.getString(R.string.external_app_not_installed),
          Toast.LENGTH_SHORT,
        )
        .show()
    }
  }
}
