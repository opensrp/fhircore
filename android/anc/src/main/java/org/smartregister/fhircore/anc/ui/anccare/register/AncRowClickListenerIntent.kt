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

package org.smartregister.fhircore.anc.ui.anccare.register

import org.smartregister.fhircore.engine.util.ListenerIntent

/**
 * This subclass of [ListenerIntent] is used to declare intention when an element of the item row is
 * clicked. To avoid string constants and also to ensure that all intentions are handled, sealed
 * interface is used.
 */
sealed interface AncRowClickListenerIntent : ListenerIntent

/** This implies that the click action is meant to launch the form to record vaccine */
object RecordAncVisit : AncRowClickListenerIntent

/** This implies that the user intends to open patient's profile */
object OpenPatientProfile : AncRowClickListenerIntent
