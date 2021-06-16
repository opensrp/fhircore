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

package org.smartregister.fhircore.fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import org.smartregister.fhircore.R

class QuestionnaireResponseDialogFragment() : DialogFragment() {

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val contents = requireArguments().getString(BUNDLE_KEY_CONTENTS)
    return activity?.let {
      val view =
        requireActivity()
          .layoutInflater
          .inflate(R.layout.questionnaire_response_dialog_contents, null)
      view.findViewById<TextView>(R.id.contents).text = contents

      AlertDialog.Builder(it).setView(view).create()
    }
      ?: throw IllegalStateException("Activity cannot be null")
  }

  companion object {
    const val TAG = "questionnaire-response-dialog-fragment"
    const val BUNDLE_KEY_CONTENTS = "contents"
  }
}
