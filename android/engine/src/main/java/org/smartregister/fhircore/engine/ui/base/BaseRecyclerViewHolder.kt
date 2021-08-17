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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.smartregister.fhircore.engine.util.ListenerIntent

/**
 * This subclass of [RecyclerView.ViewHolder] provides implementation on how to bind [Data] to the
 * view that is used to display items in [RecyclerView.Adapter]
 */
abstract class BaseRecyclerViewHolder<Data>(itemView: View) : RecyclerView.ViewHolder(itemView) {

  /**
   * Implement functionality to bind the [data] and [onItemClicked] to the [RecyclerView.ViewHolder]
   * views e.g. setting text to a TextView
   */
  abstract fun bindTo(data: Data, onItemClicked: (ListenerIntent, Data) -> Unit)
}
