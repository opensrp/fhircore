package org.smartregister.fhircore.engine.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource

abstract class BaseRegisterDataViewModel<I: Any, O : Any>(
  application: Application,
  paginatedDataSource: PaginatedDataSource<I, O>,
  pageSize: Int = 50
) : AndroidViewModel(application) {
  val registerData: Flow<PagingData<O>> =
    Pager(PagingConfig(pageSize = pageSize)) { paginatedDataSource }.flow
}
