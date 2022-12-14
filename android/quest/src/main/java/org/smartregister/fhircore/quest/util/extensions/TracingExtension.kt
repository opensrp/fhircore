package org.smartregister.fhircore.quest.util.extensions

import org.hl7.fhir.r4.model.Task

fun Task.isHomeTracingTask(): Boolean {
  return this.meta.tag.firstOrNull {
    it.`is`("https://d-tree.org", "home-tracing")
  } !== null
}

fun Task.isPhoneTracingTask(): Boolean {
  return this.meta.tag.firstOrNull {
    it.`is`("https://d-tree.org", "phone-tracing")
  } !== null
}