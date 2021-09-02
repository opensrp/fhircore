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

package org.smartregister.fhircore.engine.ui.base

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.smartregister.fhircore.engine.ui.base.BaseRecyclerViewAdapter.DataDiffCallback
import org.smartregister.fhircore.engine.util.ListenerIntent

/**
 * Subclass of [ListAdapter] that is used to provide an adapter class for the RecyclerView.
 *
 * @property onItemClicked Listener called when a row item is clicked. As many elements of the row
 * can be clicked, [ListenerIntent] is used to differentiate the actions to be performed.
 * Additionally, [Data] is passed when the listener is called. when implementing
 * [onCreateViewHolder] remember to bind the data to the view as tag for ease of use.
 *
 * @param dataDiffCallback Subclass of [DataDiffCallback] that is used to compare list row [Data]
 */
abstract class BaseRecyclerViewAdapter<Data : Any>(
  private val onItemClicked: (ListenerIntent, Data) -> Unit,
  dataDiffCallback: DataDiffCallback<Data>
) : PagingDataAdapter<Data, BaseRecyclerViewHolder<Data>>(dataDiffCallback) {

  abstract override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseRecyclerViewHolder<Data>

  override fun onBindViewHolder(holder: BaseRecyclerViewHolder<Data>, position: Int) {
    getItem(position)?.let { holder.bindTo(it, onItemClicked) }
  }

  abstract class DataDiffCallback<Data> : DiffUtil.ItemCallback<Data>()
}
