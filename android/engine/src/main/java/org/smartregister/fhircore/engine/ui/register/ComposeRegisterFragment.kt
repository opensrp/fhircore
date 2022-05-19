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

package org.smartregister.fhircore.engine.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.emptyFlow
import org.smartregister.fhircore.engine.configuration.view.ConfigurableComposableView
import org.smartregister.fhircore.engine.configuration.view.RegisterViewConfiguration
import org.smartregister.fhircore.engine.ui.components.PaginatedRegister
import org.smartregister.fhircore.engine.ui.components.register.LoaderDialog
import org.smartregister.fhircore.engine.ui.theme.AppTheme

abstract class ComposeRegisterFragment<I : Any, O : Any> :
  BaseRegisterFragment<I, O>(), ConfigurableComposableView<RegisterViewConfiguration> {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) =
    ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          val registerData = registerDataViewModel.registerData.collectAsState(emptyFlow())
          val pagingItems = registerData.value.collectAsLazyPagingItems()
          val showResultsCount by registerDataViewModel.showResultsCount.observeAsState(false)
          val showLoader by registerDataViewModel.showLoader.observeAsState(false)
          val showHeader by registerDataViewModel.showHeader.observeAsState(true)
          val showFooter by registerDataViewModel.showFooter.observeAsState(true)

          val modifier = Modifier
          if (showLoader) LoaderDialog(modifier = Modifier)
          PaginatedRegister(
            loadState = pagingItems.loadState.refresh,
            showResultsCount = showResultsCount,
            resultCount = pagingItems.itemCount,
            showHeader = showHeader,
            body = { ConstructRegisterList(pagingItems, modifier) },
            showFooter = showFooter,
            currentPage = registerDataViewModel.currentPage(),
            pagesCount = registerDataViewModel.countPages(),
            previousButtonClickListener = { registerDataViewModel.previousPage() },
            nextButtonClickListener = { registerDataViewModel.nextPage() },
            modifier = modifier,
          )
        }
      }
    }

  @Composable
  abstract fun ConstructRegisterList(pagingItems: LazyPagingItems<O>, modifier: Modifier)

  override fun configureViews(viewConfiguration: RegisterViewConfiguration) {
    registerDataViewModel.updateViewConfigurations(viewConfiguration)
  }
}
