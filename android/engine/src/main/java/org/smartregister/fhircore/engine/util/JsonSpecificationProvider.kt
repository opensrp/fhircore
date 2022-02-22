package org.smartregister.fhircore.engine.util

import kotlinx.serialization.json.Json

interface JsonSpecificationProvider {
  fun getJson(): Json
}