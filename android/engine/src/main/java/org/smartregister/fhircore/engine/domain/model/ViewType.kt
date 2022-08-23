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

package org.smartregister.fhircore.engine.domain.model

import kotlinx.serialization.json.JsonNames

/** Represents different types of views that can be rendered via compose */
enum class ViewType {
  /** Represent a vertical layout that arranges views one on top of the other */
  @JsonNames("column", "Column") COLUMN,

  /** Represents a text view that displays two texts that can be formatted separately */
  @JsonNames("compound_text", "CompoundText") COMPOUND_TEXT,

  /** A horizontal layout that arranges views from left to right */
  @JsonNames("row", "Row") ROW,

  /** A card like view with an actionable service button used register list rows */
  @JsonNames("service_card", "ServiceCard") SERVICE_CARD,

  /** Display a pair of compund texts with the formats label and displayValue */
  @JsonNames("personal_data", "PersonalData") PERSONAL_DATA,

  /** Renders a card */
  @JsonNames("card", "Card") CARD,

  /** View component used to render a button for click actions */
  @JsonNames("button", "Button") BUTTON,

  /** View component used to render a space between views */
  @JsonNames("spacer", "Spacer") SPACER,

  /** A type of view component used to render items in a list */
  @JsonNames("list", "List") LIST,
}
