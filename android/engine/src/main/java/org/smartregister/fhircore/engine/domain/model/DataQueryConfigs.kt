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

package org.smartregister.fhircore.engine.domain.model

import android.os.Parcelable
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import com.google.android.fhir.search.Operation
import com.google.android.fhir.search.StringFilterModifier
import java.math.BigDecimal
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.hl7.fhir.r4.model.Enumerations.DataType
import org.smartregister.fhircore.engine.util.serializers.BigDecimalSerializer
import org.smartregister.fhircore.engine.util.serializers.FilterCriterionSerializer

@Serializable
@Parcelize
data class DataQuery(
  val paramName: String,
  val operation: Operation = Operation.AND,
  val filterCriteria: List<FilterCriterionConfig>
) : Parcelable

@Serializable(with = FilterCriterionSerializer::class)
@Parcelize
sealed class FilterCriterionConfig : Parcelable {

  abstract val dataType: DataType

  @Serializable
  @Parcelize
  data class QuantityFilterCriterionConfig(
    override val dataType: DataType = DataType.QUANTITY,
    val prefix: ParamPrefixEnum? = null,
    @Serializable(with = BigDecimalSerializer::class) val value: BigDecimal? = null,
    val system: String? = null,
    val unit: String? = null
  ) : FilterCriterionConfig(), Parcelable

  @Serializable
  @Parcelize
  data class DateFilterCriterionConfig(
    override val dataType: DataType = DataType.DATETIME,
    val prefix: ParamPrefixEnum = ParamPrefixEnum.GREATERTHAN_OR_EQUALS,
    val valueDate: String? = null,
    val valueDateTime: String? = null
  ) : FilterCriterionConfig(), Parcelable

  @Serializable
  @Parcelize
  data class NumberFilterCriterionConfig(
    override val dataType: DataType = DataType.DECIMAL,
    val prefix: ParamPrefixEnum = ParamPrefixEnum.EQUAL,
    @Serializable(with = BigDecimalSerializer::class) val value: BigDecimal? = null
  ) : FilterCriterionConfig(), Parcelable
  @Serializable
  @Parcelize
  data class StringFilterCriterionConfig(
    override val dataType: DataType = DataType.STRING,
    val modifier: StringFilterModifier = StringFilterModifier.STARTS_WITH,
    val value: String? = null
  ) : FilterCriterionConfig(), Parcelable

  @Serializable
  @Parcelize
  data class UriFilterCriterionConfig(
    override val dataType: DataType = DataType.URI,
    val value: String? = null
  ) : FilterCriterionConfig(), Parcelable

  @Serializable
  @Parcelize
  data class ReferenceFilterCriterionConfig(
    override val dataType: DataType = DataType.REFERENCE,
    val value: String? = null
  ) : FilterCriterionConfig(), Parcelable

  @Serializable
  @Parcelize
  data class TokenFilterCriterionConfig(
    override val dataType: DataType = DataType.CODE,
    val value: Code? = null
  ) : FilterCriterionConfig(), Parcelable
}
