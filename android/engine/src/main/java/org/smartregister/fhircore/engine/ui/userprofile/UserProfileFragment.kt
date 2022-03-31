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

package org.smartregister.fhircore.engine.ui.userprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.engine.util.extension.refresh
import org.smartregister.fhircore.engine.util.extension.setAppLocale

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

  val userProfileViewModel: UserProfileViewModel by viewModels()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return ComposeView(requireContext()).apply { setContent { AppTheme { UserProfileScreen() } } }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    userProfileViewModel.onLogout.observe(viewLifecycleOwner) { shouldLogout: Boolean? ->
      if (shouldLogout != null && shouldLogout)
        AlertDialogue.showProgressAlert(requireActivity(), R.string.logging_out)
    }

    userProfileViewModel.language.observe(viewLifecycleOwner) { language: Language? ->
      if (language == null) return@observe

      setLanguageAndRefresh(language)
    }
  }

  fun setLanguageAndRefresh(language: Language) {
    val activityContext = requireActivity()
    activityContext.setAppLocale(language.tag)
    activityContext.refresh()
  }

  companion object {
    const val TAG = "UserProfileFragment"
  }
}
