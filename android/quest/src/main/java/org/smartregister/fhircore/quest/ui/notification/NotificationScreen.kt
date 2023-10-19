package org.smartregister.fhircore.quest.ui.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.paging.compose.LazyPagingItems
import org.smartregister.fhircore.engine.R
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.quest.ui.notification.components.NotificationCardList
import org.smartregister.fhircore.quest.ui.register.NoRegisterDataView
import org.smartregister.fhircore.quest.util.extensions.handleClickEvent

@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onEvent: (NotificationEvent) -> Unit,
    registerUiState: NotificationUiState,
    searchText: MutableState<String>,
    currentPage: MutableState<Int>,
    pagingItems: LazyPagingItems<ResourceData>,
    navController: NavController,
) {
    val lazyListState: LazyListState = rememberLazyListState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.notification)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, null)
                    }
                },
                contentColor = Color.White,
                backgroundColor = MaterialTheme.colors.primary,
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            if (
                registerUiState.totalRecordsCount > 0 &&
                registerUiState.registerConfiguration?.registerCard != null
            ) {
                NotificationCardList(
                    registerCardConfig = registerUiState.registerConfiguration.registerCard,
                    pagingItems = pagingItems,
                    navController = navController,
                    lazyListState = lazyListState,
                    onEvent = onEvent,
                    registerUiState = registerUiState,
                    currentPage = currentPage,
                    showPagination = searchText.value.isEmpty(),
                )
            } else {
                registerUiState.registerConfiguration?.noResults?.let { noResultConfig ->
                    NoRegisterDataView(modifier = modifier, noResults = noResultConfig) {
                        noResultConfig.actionButton?.actions?.handleClickEvent(navController)
                    }
                }
            }
        }
    }
}