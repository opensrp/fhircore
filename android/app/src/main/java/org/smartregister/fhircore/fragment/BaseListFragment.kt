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

package org.smartregister.fhircore.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.util.hide
import org.smartregister.fhircore.util.show
import timber.log.Timber

enum class Direction {
  NEXT,
  PREVIOUS
}

class PaginationView(
  val paginationView: ViewGroup,
  val nextButton: Button,
  val prevButton: Button,
  val infoTextView: TextView,
  val pageSize: Int = 7,
  val onNavigationClicked: (direction: Direction, page: Int, pageSize: Int) -> Unit
) {
  var activePageNum = 0

  fun updatePagination(pagination: Pagination) {
    activePageNum = pagination.currentPage
    nextButton.setOnClickListener {
      onNavigationClicked.invoke(Direction.NEXT, pagination.currentPage, pageSize)
    }
    prevButton.setOnClickListener {
      onNavigationClicked.invoke(Direction.PREVIOUS, pagination.currentPage, pageSize)
    }

    nextButton.visibility = if (pagination.hasNextPage()) View.GONE else View.VISIBLE
    prevButton.visibility = if (pagination.hasPreviousPage()) View.GONE else View.VISIBLE
    paginationView.visibility =
      if (nextButton.visibility == View.VISIBLE || prevButton.visibility == View.VISIBLE)
        View.VISIBLE
      else View.GONE

    this.infoTextView.text =
      if (pagination.totalPages() < 2) ""
      else
        paginationView.resources.getString(
          R.string.str_page_info,
          pagination.currentPageNumber(),
          pagination.totalPages()
        )
  }

  companion object {
    const val DEFAULT_PAGE_SIZE = 20

    fun createPaginationView(
      container: ViewGroup,
      onNavigation: (direction: Direction, page: Int, pageSize: Int) -> Unit
    ): PaginationView {
      val paginationView =
        LayoutInflater.from(container.context).inflate(R.layout.pagination, container, false)
      return PaginationView(
        paginationView as ViewGroup,
        pageSize = 7,
        infoTextView = paginationView.findViewById(R.id.txt_page_info),
        nextButton = paginationView.findViewById(R.id.btn_next_page),
        prevButton = paginationView.findViewById(R.id.btn_previous_page),
        onNavigationClicked = onNavigation
      )
    }
  }
}

abstract class BaseListFragment<T : Any?, VH : RecyclerView.ViewHolder?> : Fragment() {
  var search: String? = ""
  lateinit var adapter: ListAdapter<T, VH>
  lateinit var recyclerView: RecyclerView
  var paginationView: PaginationView? = null
  var emptyListView: View? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(getFragmentListLayout(), container, false)
  }

  @LayoutRes abstract fun getFragmentListLayout(): Int

  abstract fun loadData(currentSearch: String?, page: Int, pageSize: Int)

  fun setupEmptyListView(@IdRes id: Int) {
    emptyListView = requireActivity().findViewById(id)!!
  }

  fun setupPagination(container: ViewGroup) {
    paginationView =
      PaginationView.createPaginationView(container) { d, page, pageSize ->
        Timber.i("Loading $d page #$page with records $pageSize")
        loadData(search, page, pageSize)
      }
  }

  fun setupListFragment(
    @IdRes fragmentListView: Int,
    fragmentList: MutableLiveData<Pair<List<T>, Pagination>>,
    adapter: ListAdapter<T, VH>,
    view: View
  ) {
    recyclerView = view.findViewById(fragmentListView)
    this.adapter = adapter
    recyclerView.adapter = adapter

    fragmentList.observe(requireActivity(), { setData(it) })
  }

  fun setupSearch(searchBox: EditText) {
    searchBox.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
          search = s?.toString()
          loadData(search, 0, getPageSize())
        }

        override fun afterTextChanged(s: Editable?) {}
      }
    )
  }

  fun setupProgress(@IdRes id: Int, state: MutableLiveData<Int>) {
    state.observe(
      requireActivity(),
      {
        if (it != -1) {
          requireActivity().findViewById<View>(id).visibility =
            if (it == 1) View.VISIBLE else View.GONE
        }
      }
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    kotlin.runCatching { recyclerView.adapter != null }.onFailure {
      throw IllegalStateException(
        "You must initialize adapter and recyclerview by calling #setupListFragment as first method"
      )
    }

    loadData("", 0, getPageSize()) // todo sync first somewhere blocking the ui
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onResume() {
    loadData(search, 0, getPageSize())
    adapter.notifyDataSetChanged()
    super.onResume()
  }

  fun getPageSize(): Int {
    return paginationView?.pageSize ?: PaginationView.DEFAULT_PAGE_SIZE
  }

  fun setData(data: Pair<List<T>, Pagination>) {
    Timber.d("rendering ${data.first.count()} patient records")
    val list = ArrayList<T>(data.first)
    paginationView?.updatePagination(data.second)
    adapter.submitList(list)

    if (data.first.count() == 0) {
      emptyListView?.show()
    } else {
      emptyListView?.hide()
    }
  }
}
