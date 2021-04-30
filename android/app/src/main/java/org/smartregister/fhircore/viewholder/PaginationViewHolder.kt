/*
 * Copyright 2021 Ona Systems Inc
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

package org.smartregister.fhircore.viewholder

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.R
import org.smartregister.fhircore.domain.Pagination
import org.smartregister.fhircore.domain.currentPageNumber
import org.smartregister.fhircore.domain.hasNextPage
import org.smartregister.fhircore.domain.hasPreviousPage
import org.smartregister.fhircore.domain.totalPages
import org.smartregister.fhircore.fragment.NavigationDirection

class PaginationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  private val nextButton: Button = itemView.findViewById(R.id.btn_next_page)
  private val prevButton: Button = itemView.findViewById(R.id.btn_previous_page)
  private val infoTextView: TextView = itemView.findViewById(R.id.txt_page_info)

  fun bindTo(pagination: Pagination, onItemClicked: (NavigationDirection, Int) -> Unit) {
    nextButton.setOnClickListener {
      onItemClicked(NavigationDirection.NEXT, pagination.currentPage)
    }
    prevButton.setOnClickListener {
      onItemClicked(NavigationDirection.PREVIOUS, pagination.currentPage)
    }

    nextButton.visibility = if (pagination.hasNextPage()) View.GONE else View.VISIBLE
    prevButton.visibility = if (pagination.hasPreviousPage()) View.GONE else View.VISIBLE

    this.infoTextView.text =
      itemView.resources.getString(
        R.string.str_page_info,
        pagination.currentPageNumber(),
        pagination.totalPages()
      )
  }
}
