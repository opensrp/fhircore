/*
 * Copyright 2021-2024 Ona Systems, Inc
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

package org.smartregister.fhircore.engine.util

import java.io.IOException
import java.math.BigDecimal
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.UriType
import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.util.extension.valueToString

class FhirDataTypesUtilKtTest {
  @Test
  fun testCastToTypeReturnsCorrectTypes() {
    val booleanType = "true".castToType(DataType.BOOLEAN)
    Assert.assertEquals(BooleanType().fhirType(), booleanType?.fhirType())
    Assert.assertEquals("true", booleanType.valueToString())

    val decimalType = "6.4".castToType(DataType.DECIMAL)
    Assert.assertEquals(DecimalType().fhirType(), decimalType?.fhirType())
    Assert.assertEquals("6.4", decimalType.valueToString())

    val integerType = "4".castToType(DataType.INTEGER)
    Assert.assertEquals(IntegerType().fhirType(), integerType?.fhirType())
    Assert.assertEquals("4", integerType.valueToString())

    val dateType = "2020-02-02".castToType(DataType.DATE)
    Assert.assertEquals(DateType().fhirType(), dateType?.fhirType())
    Assert.assertEquals("02-Feb-2020", dateType.valueToString())

    val dateTimeType = "2020-02-02T13:00:32".castToType(DataType.DATETIME)
    Assert.assertEquals(DateTimeType().fhirType(), dateTimeType?.fhirType())
    Assert.assertEquals("02-Feb-2020", dateTimeType.valueToString())

    val timeType = "T13:00:32".castToType(DataType.TIME)
    Assert.assertEquals(TimeType().fhirType(), timeType?.fhirType())
    Assert.assertEquals("T13:00:32", timeType.valueToString())

    val stringType = "str".castToType(DataType.STRING)
    Assert.assertEquals(StringType().fhirType(), stringType?.fhirType())
    Assert.assertEquals("str", stringType.valueToString())

    val uriType = "https://str.org".castToType(DataType.URI)
    Assert.assertEquals(UriType().fhirType(), uriType?.fhirType())
    Assert.assertEquals("https://str.org", uriType.valueToString())

    val codingType = "{ \"code\": \"alright\" }".castToType(DataType.CODING)
    Assert.assertTrue(codingType is Coding)
    codingType as Coding
    Assert.assertEquals("alright", codingType.code)

    Assert.assertThrows(IOException::class.java) {
      val codingType = "invalid".castToType(DataType.CODING)
      Assert.assertEquals(null, codingType)
    }

    val quantityType = " { \"value\": 42 }".castToType(DataType.QUANTITY)
    Assert.assertTrue(quantityType is Quantity)
    quantityType as Quantity
    Assert.assertEquals(BigDecimal(42), quantityType.value)

    Assert.assertThrows(IOException::class.java) {
      val quantityType = "invalid".castToType(DataType.QUANTITY)
      Assert.assertEquals(null, quantityType)
    }

    val referenceType = "{ \"reference\": \"Patient/0\"}".castToType(DataType.REFERENCE)
    Assert.assertTrue(referenceType is Reference)
    referenceType as Reference
    Assert.assertEquals("Patient/0", referenceType.reference)
  }
}
