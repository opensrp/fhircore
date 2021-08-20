package org.smartregister.fhircore.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.adapter.FamilyItemRecyclerViewAdapter
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.viewmodel.FamilyListViewModel
import timber.log.Timber

abstract class BaseListFragment<T : Any?, VH : RecyclerView.ViewHolder?> : Fragment() {
  val PAGE_SIZE = 7

  private var search: String? = ""
  private lateinit var adapter: ListAdapter<T, VH>
  private lateinit var paginationView: RelativeLayout
  private lateinit var recyclerView: RecyclerView
  private lateinit var nextButton: Button
  private lateinit var prevButton: Button
  private lateinit var infoTextView: TextView
  private var activePageNum = 0

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(getFragmentListLayout(), container, false)
  }

  @LayoutRes abstract fun getFragmentListLayout(): Int

  @IdRes abstract fun getFragmentListId(): Int

  @IdRes abstract fun getEmptyListView(): Int

  abstract fun buildAdapter(): ListAdapter<T, VH>

  abstract fun getObservableList(): MutableLiveData<Pair<List<T>, Pagination>>

  abstract fun getObservableProgressBar(): Int

  open fun getRegister(): BaseRegister {
    return (requireActivity() as BaseRegisterActivity).register
  }

  abstract fun loadData(currentSearch: String?, page: Int, pageSize: Int)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    recyclerView = view.findViewById(getFragmentListId())
    paginationView = view.findViewById(R.id.rl_pagination)
    nextButton = view.findViewById(R.id.btn_next_page)
    prevButton = view.findViewById(R.id.btn_previous_page)
    infoTextView = view.findViewById(R.id.txt_page_info)
    adapter = buildAdapter()

    recyclerView.adapter = adapter

    getObservableList().observe(requireActivity(), { setData(it) })

    // todo sync should be on reg

    getRegister().searchBox()?.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            search = s?.toString()
            loadData(search, 0, PAGE_SIZE)
          }

          override fun afterTextChanged(s: Editable?) {}
        }
      )

    // todo move overdue here

    // todo loader here
//    patientListViewModel.loadingListObservable.observe(
//      requireActivity(),
//      {
//        if (it != -1) {
//          requireActivity().findViewById<ConstraintLayout>(R.id.loader_overlay).visibility =
//            if (it == 1) View.VISIBLE else View.GONE
//        }
//      }
//    )

    loadData("", 0, PAGE_SIZE) // todo sync first somewhere blocking the ui
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onResume() {
    loadData(search, 0, PAGE_SIZE)
    adapter.notifyDataSetChanged()
    super.onResume()
  }

  fun setData(data: Pair<List<T>, Pagination>) {
    Timber.d("rendering ${data.first.count()} patient records")
    val list = ArrayList<T>(data.first)
    updatePagination(data.second)
    adapter.submitList(list)

    if (data.first.count() == 0) {
      showEmptyListViews()
    } else {
      hideEmptyListViews()
    }
  }

  fun hideEmptyListViews() {
    setVisibility(getEmptyListView(), View.GONE)
  }

  fun showEmptyListViews() {
    setVisibility(getEmptyListView(), View.VISIBLE)
  }

  private fun setVisibility(id: Int, visibility: Int) {
    requireActivity().findViewById<View>(id).visibility = visibility
  }

  private fun updatePagination(pagination: Pagination) {
    activePageNum = pagination.currentPage
    nextButton.setOnClickListener {
      onNavigationClicked(NavigationDirection.NEXT, pagination.currentPage)
    }
    prevButton.setOnClickListener {
      onNavigationClicked(NavigationDirection.PREVIOUS, pagination.currentPage)
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
        resources.getString(
          R.string.str_page_info,
          pagination.currentPageNumber(),
          pagination.totalPages()
        )
  }

  private fun onNavigationClicked(direction: NavigationDirection, currentPage: Int) {
    val nextPage = currentPage + if (direction == NavigationDirection.NEXT) 1 else -1
    loadData(search, nextPage, PAGE_SIZE)
  }
}
