package org.smartregister.fhircore.engine.util.extension


fun generateRandomSixDigitInt(min: Int, max: Int): Int {
  return (min..max).shuffled().random()
}