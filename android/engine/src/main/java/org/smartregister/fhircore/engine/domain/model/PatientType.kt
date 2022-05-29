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

enum class PatientType {
  NEWLY_DIAGNOSED_CLIENT,
  CLIENT_ALREADY_ON_ART,
  EXPOSED_INFANT,
  COMMUNITY_POSITIVE,
  CHILD_CONTACT,
  SEXUAL_CONTACT,
  DEFAULT
}

// enum class PatientType(value: String, display: String = "") {
//  NEWLY_DIAGNOSED(value = "newly-diagnosed-client", display = "Newly Diagnosed Client"),
//  ALREADY_ON_ART(value = "client-already-on-art", display = "Client Already On ART"),
//  EXPOSED_INFANT(value = "exposed-infant", display = "Exposed Infant"),
//  COMMUNITY_POSITIVE(value = "community-positive", display = "Community Positive"),
//  CHILD_CONTACT(value = "child-contact", display = "Child Contact"),
//  SEXUAL_CONTACT(value = "sexual-contact", display = "Sexual Contact"),
//  DEFAULT(value = "", "Unknown")
// }



//                code: "newly-diagnosed-client", display: "Newly Diagnosed Client"
//                code: "client-already-on-art", display: "Client Already On ART"
//                code: "exposed-infant", display: "Exposed Infant"
//                code: "community-positive", display: "Community Positive"
//                code: "child-contact", display: "Child Contact"
//                code: "sexual-contact", display: "Sexual Contact"
