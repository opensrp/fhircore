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

package org.smartregister.fhircore.quest.ui.questionnaire

import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.google.android.fhir.datacapture.contrib.views.barcode.BarCodeReaderViewHolderFactory
import com.google.android.fhir.datacapture.contrib.views.locationwidget.LocationGpsCoordinateViewHolderFactory
import com.google.android.fhir.datacapture.contrib.views.locationwidget.LocationWidgetViewHolderFactory
import com.google.android.fhir.datacapture.extensions.asStringValue

const val OPENSRP_ITEM_VIEWHOLDER_FACTORY_MATCHERS_PROVIDER =
  "org.smartregister.fhircore.quest.QuestionnaireItemViewHolderFactoryMatchersProvider"

object QuestionnaireItemViewHolderFactoryMatchersProviderFactoryImpl :
  QuestionnaireItemViewHolderFactoryMatchersProviderFactory {

  override fun get(
    provider: String,
  ): QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider {
    return when (provider) {
      OPENSRP_ITEM_VIEWHOLDER_FACTORY_MATCHERS_PROVIDER ->
        OpenSRPQuestionnaireItemViewHolderFactoryMatchersProviderImpl
      else -> throw NotImplementedError()
    }
  }

  object OpenSRPQuestionnaireItemViewHolderFactoryMatchersProviderImpl :
    QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider() {

    const val BARCODE_URL = "https://smartregister.org/barcode-type-widget-extension"
    const val BARCODE_NAME = "barcode"

    override fun get(): List<QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher> {
      return listOf(
        QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
          BarCodeReaderViewHolderFactory,
        ) { questionnaireItem ->
          questionnaireItem.getExtensionByUrl(BARCODE_URL).let {
            if (it == null) false else it.value.asStringValue() == BARCODE_NAME
          }
        },
        QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
          factory = LocationGpsCoordinateViewHolderFactory,
          matches = LocationGpsCoordinateViewHolderFactory::matcher,
        ),
        QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
          factory = LocationWidgetViewHolderFactory,
          matches = LocationWidgetViewHolderFactory::matcher,
        ),
      )
    }
  }
}
