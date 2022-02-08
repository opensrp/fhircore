package org.smartregister.fhircore.mwcore.ui.patient.register.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items
import org.smartregister.fhircore.engine.ui.components.CircularProgressBar
import org.smartregister.fhircore.engine.ui.components.ErrorMessage
import org.smartregister.fhircore.engine.ui.theme.DividerColor
import org.smartregister.fhircore.mwcore.data.patient.model.PatientItem
import org.smartregister.fhircore.mwcore.ui.patient.register.PatientRowClickListenerIntent

@Composable
fun ExposedRegisterList(
    pagingItems: LazyPagingItems<PatientItem>,
    modifier: Modifier = Modifier,
    clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit
) {
    LazyColumn {
        items(pagingItems, key = { it.id }) {
            PatientChildRow(it!!, clickListener, modifier = modifier)
            Divider(color = DividerColor, thickness = 1.dp)
        }

        pagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading ->
                    item { CircularProgressBar(modifier = modifier.fillParentMaxSize()) }
                loadState.append is LoadState.Loading -> item { CircularProgressBar() }
                loadState.refresh is LoadState.Error -> {
                    val loadStateError = pagingItems.loadState.refresh as LoadState.Error
                    item {
                        ErrorMessage(
                            message = loadStateError.error.localizedMessage!!,
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


