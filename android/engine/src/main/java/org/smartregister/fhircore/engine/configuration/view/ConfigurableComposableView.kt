package org.smartregister.fhircore.engine.configuration.view

import org.smartregister.fhircore.engine.configuration.Configuration

/**
 * [ConfigurableComposableView] interface provides the contract for configuring Composable views A
 * subclass of [Configuration] is used to supply view configurations to the view model. Every
 * composable screen needs to extend the [Configuration] class.
 */
interface ConfigurableComposableView<T : Configuration> {

  /**
   * This method is used to update the [viewConfiguration]. E.g. would be toggling the visibility of
   * a view or changing the text color e.t.c. [Configuration] The subclasses con override the
   * default configurations of their base classes implementing this method to provide their own.
   */
  fun configureViews(viewConfiguration: T)
}
