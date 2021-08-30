package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import org.smartregister.fhircore.engine.ui.theme.AppTheme

abstract class ComposeRegisterFragment<I : Any, O : Any> : BaseRegisterFragment<I, O>() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = ComposeView(requireContext()).apply { setContent { AppTheme { ConstructRegisterList() } } }

  @Composable abstract fun ConstructRegisterList()
}
