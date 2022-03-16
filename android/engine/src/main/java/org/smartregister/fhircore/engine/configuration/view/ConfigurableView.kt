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

package org.smartregister.fhircore.engine.configuration.view

import org.smartregister.fhircore.engine.configuration.Configuration

/**
 * [ConfigurableView] interface provides the contract for configuring UI elements. A subclass of
 * [Configuration] is used to supply view configurations to the view model. Every customizable UI
 * using the imperative Android views is required to extend the [Configuration] class. It is
 * recommended to have all the implementation to the base classes so that the subclasses will not
 * have to worry about providing their own implementations.
 */
interface ConfigurableView<T : Configuration> {

  /**
   * This method is used to update the [viewConfiguration]. E.g. would be toggling the visibility of
   * a view or changing the text color e.t.c. [Configuration]. The subclasses con override the
   * default configurations of their base classes implementing this method to provide their own.
   */
  fun configureViews(viewConfiguration: T)

  /**
   * This method is called mainly in the base classes to apply the configurations provided in the
   * [viewConfiguration]. The actual implementation for setting the text color for instance is done
   * in this method. The default functionality provided by the implementers of this subclass can can
   * be overridden by the subclasses
   */
  fun setupConfigurableViews(viewConfiguration: T)
}
