package org.smartregister.fhircore.engine.ui.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.smartregister.fhircore.engine.data.domain.util.PaginatedDataSource
import org.smartregister.fhircore.engine.ui.register.model.RegisterFilterType
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.DispatcherProvider

abstract class BaseRegisterDataViewModel<I : Any, O : Any>(
  application: Application,
  val paginatedDataSource: PaginatedDataSource<I, O>,
  pageSize: Int = 50,
  private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider
) : AndroidViewModel(application) {

  private val originalData = Pager(PagingConfig(pageSize = pageSize)) { paginatedDataSource }.flow

  val registerData: MutableLiveData<Flow<PagingData<O>>> by lazy { MutableLiveData(originalData) }

  /**
   * Perform filter on the paginated data by invoking the [registerFilter] lambda, passing
   * [filterValue] and [registerFilterType] as the type of filter that you wish to apply e.g.
   * searching by name filter, filter overdue vaccines etc.
   */
  fun filterRegisterData(
    registerFilterType: RegisterFilterType,
    filterValue: Any,
    registerFilter: (RegisterFilterType, O, Any) -> Boolean
  ) {
    viewModelScope.launch(dispatcherProvider.io()) {
      registerData.postValue(
        originalData.map { pagingData: PagingData<O> ->
          pagingData.filter { registerFilter(registerFilterType, it, filterValue) }
        }
      )
    }
  }
}
