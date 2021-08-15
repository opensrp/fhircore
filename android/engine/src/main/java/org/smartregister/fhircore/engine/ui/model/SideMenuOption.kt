package org.smartregister.fhircore.engine.ui.model

import android.graphics.drawable.Drawable

/**
 * @property itemId Unique number used to identify the menu item.
 * @property titleResource Android translatable string resource used as the menu option title
 * @property iconResource Android drawable resource used as icon for menu option
 * @property opensMainRegister Use current option to open the main the register
 * @property showCount Show clients count against the menu option
 */
data class SideMenuOption(
  val itemId: Int,
  val titleResource: Int,
  val iconResource: Drawable,
  val opensMainRegister: Boolean = false,
  val showCount: Boolean = true,
)
