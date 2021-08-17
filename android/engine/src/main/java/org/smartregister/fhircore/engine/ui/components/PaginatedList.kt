package org.smartregister.fhircore.engine.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemsIndexed
import org.smartregister.fhircore.engine.ui.theme.DividerColor

@Composable
fun <O : Any> PaginatedList(
  pagingItems: LazyPagingItems<O>,
  itemRow: @Composable (O) -> Unit,
  modifier: Modifier = Modifier
) {

  LazyColumn {
    itemsIndexed(items = pagingItems, key = { index, _ -> index }) { _, item ->
      itemRow(item!!)
      Divider(color = DividerColor, thickness = 1.dp)
    }

    pagingItems.apply {
      when {
        loadState.refresh is LoadState.Loading -> {
          item { CircularProgressBar(modifier = modifier.fillParentMaxSize()) }
        }
        loadState.append is LoadState.Loading -> {
          item { CircularProgressBar() }
        }
        loadState.refresh is LoadState.Error -> {
          val e = pagingItems.loadState.refresh as LoadState.Error
          item {
            ErrorMessage(
              message = e.error.localizedMessage!!,
              modifier = modifier.fillParentMaxSize(),
              onClickRetry = { retry() }
            )
          }
        }
        loadState.append is LoadState.Error -> {
          val error = pagingItems.loadState.append as LoadState.Error
          item {
            ErrorMessage(message = error.error.localizedMessage!!, onClickRetry = { retry() })
          }
        }
      }
    }
  }
}
