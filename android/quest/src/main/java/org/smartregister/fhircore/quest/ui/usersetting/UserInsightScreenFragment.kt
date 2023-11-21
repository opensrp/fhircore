package org.smartregister.fhircore.quest.ui.usersetting

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.smartregister.fhircore.engine.BuildConfig
import org.smartregister.fhircore.engine.ui.theme.AppTheme
import org.smartregister.fhircore.quest.R

@AndroidEntryPoint
class UserInsightScreenFragment : Fragment() {
    private val userSettingViewModel by viewModels<UserSettingViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    UserSettingInsightScreen(
                        unsyncedResources = userSettingViewModel.unsyncedResourcesMutableSharedFlow.collectAsState(initial = listOf()).value,
                        syncedResources = userSettingViewModel.unsyncedResourcesMutableSharedFlow.collectAsState(initial = listOf()).value,
                        mainNavController =  findNavController()
                    )
                }
            }
        }
    }

}