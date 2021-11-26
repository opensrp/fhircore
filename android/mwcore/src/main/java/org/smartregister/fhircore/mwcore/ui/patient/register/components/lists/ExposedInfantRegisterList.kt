package org.smartregister.fhircore.mwcore.ui.patient.register.components.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import org.smartregister.fhircore.mwcore.ui.patient.register.components.list_items.ExposedInfantListItem

@Composable
fun ExposedInfantRegisterList(
    pagingItems: LazyPagingItems<PatientItem>,
    modifier: Modifier = Modifier,
    clickListener: (PatientRowClickListenerIntent, PatientItem) -> Unit
) {
    if (pagingItems.itemCount == 0) {
        Column(modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No Exposed Infants Found")
        }
        return
    }

    LazyColumn {
        items(pagingItems, key = { it.id }) {
            ExposedInfantListItem(it!!, clickListener, modifier = modifier)
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