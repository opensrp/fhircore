/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.quest.ui.shared

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.ActionParameter
import org.smartregister.fhircore.quest.ui.questionnaire.QuestionnaireActivity

interface PermissionHandler {

  val startForPermissionsResult: ActivityResultLauncher<Array<String>>

  /** This function launches PermissionRequest and returns [ActivityResult] on action. */
  fun launchPermissionRequest(
    permissions: List<String>,
  )  = startForPermissionsResult.launch(permissions.toTypedArray())

  /** This function check permission granted status and return true if there is any permission which is not
   * granted otherwise return false */
  fun needPermissionRequest(context: Context, permissions: List<String>) =
    permissions.any {  permission ->
      ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

  /** This function retrieve available permissions registered in Manifest. */
  fun retrievePermissions(context: Context): List<String> {
    return try {
        context.packageManager
          .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
          .requestedPermissions.toList()
    } catch (e: Exception) {
      listOf()
    }
  }

  /** This function retrieve permission info and return human readable name. */
  fun getPermissionInfo(context: Context, permission: String): String =
    context.packageManager.run {
      getPermissionInfo(permission, 0).loadLabel(this).toString()
    }

  fun handlePermissions(context: Context, permissions: List<String>? = null)
  fun onGrantedPermissions()
}
