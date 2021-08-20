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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking
import org.smartregister.fhircore.R
import org.smartregister.fhircore.activity.core.BaseRegisterActivity
import org.smartregister.fhircore.adapter.FamilyItemRecyclerViewAdapter
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.model.BaseRegister
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.viewmodel.FamilyListViewModel
import timber.log.Timber

class FamilyListFragment : Fragment() {
    private var search: String? = ""
    internal val listViewModel by activityViewModels<FamilyListViewModel>()
    private lateinit var adapter: FamilyItemRecyclerViewAdapter
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
        return inflater.inflate(R.layout.family_fragment_list, container, false)
    }

    open fun getRegister(): BaseRegister {
        return (requireActivity() as BaseRegisterActivity).register
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.family_list)
        adapter = FamilyItemRecyclerViewAdapter(this::onFamilyItemClicked)
        paginationView = view.findViewById(R.id.rl_pagination)
        nextButton = view.findViewById(R.id.btn_next_page)
        prevButton = view.findViewById(R.id.btn_previous_page)
        infoTextView = view.findViewById(R.id.txt_page_info)

        recyclerView.adapter = adapter

        listViewModel.paginatedDataList.observe(requireActivity(), { setData(it) })

        getRegister().searchBox()?.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    search = s?.toString()
                    listViewModel.searchResults(search, 0, PAGE_COUNT)
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )

        loadData()
        super.onViewCreated(view, savedInstanceState)
    }

    internal fun loadData() {
        runBlocking {
            if (listViewModel.count("") == 0L) {
                listViewModel.searchResults("") // todo runSync(true)
            } else listViewModel.searchResults("")
        }
    }

    enum class Intention {
        RECORD_DATA,
        VIEW
    }

    fun onFamilyItemClicked(intention: FamilyListFragment.Intention, patientItem: FamilyItem) {

    }

    fun hideEmptyListViews() {
        setVisibility(R.id.empty_list_message_container, View.GONE)
    }

    fun showEmptyListViews() {
        setVisibility(R.id.empty_list_message_container, View.VISIBLE)
    }

    private fun setVisibility(id: Int, visibility: Int) {
        requireActivity().findViewById<View>(id).visibility = visibility
    }

    // Click handler to help display the details about the patients from the list.
    private fun onNavigationClicked(direction: NavigationDirection, currentPage: Int) {
        val nextPage = currentPage + if (direction == NavigationDirection.NEXT) 1 else -1
        // todo listViewModel.searchResults(search, nextPage, PAGE_COUNT)
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

    fun setData(data: Pair<List<FamilyItem>, Pagination>) {
        Timber.d("Submitting ${data.first.count()} patient records")
        val list = ArrayList<FamilyItem>(data.first)
        updatePagination(data.second)
        adapter.submitList(list)

        if (data.first.count() == 0) {
            showEmptyListViews()
        } else {
            hideEmptyListViews()
        }
    }
}
