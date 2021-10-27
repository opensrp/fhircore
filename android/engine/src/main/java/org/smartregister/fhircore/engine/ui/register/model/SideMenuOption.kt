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

package org.smartregister.fhircore.engine.ui.register.model

import android.graphics.drawable.Drawable

/**
 * @property itemId Unique number used to identify the menu item.
 * @property titleResource Android translatable string resource used as the menu option title
 * @property iconResource Android drawable resource used as icon for menu option
 * @property count The current count for the menu item. Default is 0
 * @property showCount Show clients count against the menu option queries for resources other than
 * Patient
 * @property countMethod High order function used to return count for the menu option. Defaults to
 * returning -1. -1 is used to determine whether to count active patients or not. Override to
 * provide custom count implementation for instance calling a view model method to perform the count
 */
data class SideMenuOption(
  val itemId: Int,
  val titleResource: Int,
  val iconResource: Drawable,
  var count: Long = 0,
  val showCount: Boolean = true,
  val countMethod: () -> Long = { -1 }
)
