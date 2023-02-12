/*
 * Copyright 2021-2023 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.configuration

/**
 * Every class or object providing UI customizations e.g. appTitle, showFilter, showSideMenu,
 * showSearchBar etc. is required MUST adhere to this contract to provide consistencies.
 * Conventionally, the implementers should be named after this interface e.g.
 * RegisterViewConfiguration, ProfileViewConfiguration etc.
 *
 * @property appId Unique identifier for the application to which this configurations is applied
 * @property configType Used to categorize multiple configurations of the same type. E.g. two
 * RegisterViewConfigurations used in an application with two registers.
 * @property resourceType Optional FHIR resource type
 */
abstract class Configuration {
  open lateinit var appId: String
  open lateinit var configType: String
  open val resourceType: String? = null
}
