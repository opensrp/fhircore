package org.smartregister.fhircore.engine.configuration

/**
 * Every class or object providing UI customizations e.g. appTitle, showFilter, showSideMenu,
 * showSearchBar etc. is required MUST adhere to this contract to provide consistencies.
 * Conventionally, the implementers should be named after this interface e.g.
 * RegisterViewConfiguration, ProfileViewConfiguration etc. [viewClass] method just returns the
 * Class implementing this interface.
 */
interface Configuration {
  fun viewClass() = this::class
}
