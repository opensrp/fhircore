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
import org.smartregister.fhircore.adapter.AncItemRecyclerViewAdapter
import org.smartregister.fhircore.adapter.FamilyItemRecyclerViewAdapter
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.model.FamilyItem
import org.smartregister.fhircore.model.PatientItem
import org.smartregister.fhircore.viewmodel.AncListViewModel
import org.smartregister.fhircore.viewmodel.FamilyListViewModel
import timber.log.Timber

class Anc2ListFragment : Fragment() {
    internal val listViewModel by activityViewModels<AncListViewModel>()
    private lateinit var adapter: AncItemRecyclerViewAdapter
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
        return inflater.inflate(R.layout.anc_fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.anc_list)
        adapter = AncItemRecyclerViewAdapter(this::onFamilyItemClicked)
        paginationView = view.findViewById(R.id.rl_pagination)
        nextButton = view.findViewById(R.id.btn_next_page)
        prevButton = view.findViewById(R.id.btn_previous_page)
        infoTextView = view.findViewById(R.id.txt_page_info)

        recyclerView.adapter = adapter

        listViewModel.paginatedDataList.observe(requireActivity(), { setData(it) })

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

    fun onFamilyItemClicked(patientItem: PatientItem) {

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

    fun setData(data: Pair<List<PatientItem>, Pagination>) {
        Timber.d("Submitting ${data.first.count()} patient records")
        val list = ArrayList<PatientItem>(data.first)
        updatePagination(data.second)
        adapter.submitList(list)

        if (data.first.count() == 0) {
            showEmptyListViews()
        } else {
            hideEmptyListViews()
        }
    }
}
