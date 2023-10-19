package org.smartregister.fhircore.quest.ui.notification.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import org.smartregister.fhircore.engine.configuration.register.RegisterCardConfig
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.components.register.RegisterFooter
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.quest.ui.notification.NotificationEvent
import org.smartregister.fhircore.quest.ui.notification.NotificationUiState
import org.smartregister.fhircore.quest.ui.shared.components.ViewRenderer
import timber.log.Timber

const val NOTIFICATION_CARD_LIST_TEST_TAG = "NotificationCardListTestTag"

/**
 * This is the list used to render register data. The register data is wrapped in [ResourceData]
 * class. Each row of the register is then rendered based on the provided [RegisterCardConfig]
 */
@Composable
fun NotificationCardList(
    modifier: Modifier = Modifier,
    registerCardConfig: RegisterCardConfig,
    pagingItems: LazyPagingItems<ResourceData>,
    navController: NavController,
    lazyListState: LazyListState,
    onEvent: (NotificationEvent) -> Unit,
    registerUiState: NotificationUiState,
    currentPage: MutableState<Int>,
    showPagination: Boolean = false,
) {
    LazyColumn(modifier = modifier.testTag(NOTIFICATION_CARD_LIST_TEST_TAG), state = lazyListState) {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.baseResourceId },
            contentType = pagingItems.itemContentType(),
        ) { index ->
            // Register card UI rendered dynamically should be wrapped in a column
            val data = pagingItems[index]!!
            Column(modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        onEvent(
                            NotificationEvent.ShowNotification(
                                data = data.computedValuesMap,
                                id = data.baseResourceId
                            )
                        )
                    }
            ) {
                ViewRenderer(
                    viewProperties = registerCardConfig.views,
                    resourceData = data,
                    navController = navController,
                )
            }
            Divider(color = DividerColor, thickness = 1.dp)
        }
        pagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading -> item { CircularProgressBar() }
                loadState.append is LoadState.Loading -> item { CircularProgressBar() }
                loadState.refresh is LoadState.Error -> {
                    val loadStateError = pagingItems.loadState.refresh as LoadState.Error
                    item {
                        ErrorMessage(
                            message = loadStateError.error.also { Timber.e(it) }.localizedMessage!!,
                            onClickRetry = { retry() },
                        )
                    }
                }

                loadState.append is LoadState.Error -> {
                    val error = pagingItems.loadState.append as LoadState.Error
                    item {
                        ErrorMessage(
                            message = error.error.localizedMessage!!,
                            onClickRetry = { retry() })
                    }
                }
            }
        }

        // Register pagination
        item {
            if (pagingItems.itemCount > 0 && showPagination) {
                RegisterFooter(
                    resultCount = pagingItems.itemCount,
                    currentPage = currentPage.value.plus(1),
                    pagesCount = registerUiState.pagesCount,
                    previousButtonClickListener = { onEvent(NotificationEvent.MoveToPreviousPage) },
                    nextButtonClickListener = { onEvent(NotificationEvent.MoveToNextPage) },
                )
            }
        }
    }
}