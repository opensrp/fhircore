package org.smartregister.fhircore.engine.configuration.view

import android.view.View
import org.smartregister.fhircore.engine.configuration.Configuration

/**
 * [ConfigurableView] interface provides the contract for configuring UI elements. A subclass of
 * [Configuration] is used to supple view configurations to the view model. Every customizable
 * UI must extend the [Configuration] class. [configurableViews] is a map used to hold reference
 * to all the customizable views, this is useful when you need override the default implementation
 * provided by the base class. It is recommended to have all the implementation to the base classes
 * so that the subclasses will not have to worry about providing their own implementations.
 */
interface ConfigurableView<T : Configuration> {

  val configurableViews: Map<String, View>

  /**
   * This method is used to update the [viewConfiguration]. E.g. would be toggling the visibility of
   * a view or changing the text color e.t.c. [Configuration] also contains a map for all the
   * customizable views. The subclasses con override the default configurations of their base
   * classes implementing this method to provide their own.
   */
  fun configureViews(viewConfiguration: T)

  /**
   * This method is called mainly in the base classes to apply the configurations provided in the
   * [viewConfiguration]. The actual implementation for setting the text color for instance is done
   * in this method. The default functionality provided by the implementers of this subclass can can
   * be overridden by the subclasses
   */
  fun setupConfigurableViews(viewConfiguration: RegisterViewConfiguration)
}
