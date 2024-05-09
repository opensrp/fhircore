package org.smartregister.fhircore.quest.ui.register.matcher

import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireItemViewHolderFactoryMatchersProviderFactory

object CustomChipItemViewHolderFactoryMatchersProviderFactory : QuestionnaireItemViewHolderFactoryMatchersProviderFactory {

    const val RADIO_BUTTON_PROVIDER = "radio-button"

    override fun get(
        provider: String,
    ): QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider =
        when (provider) {
            RADIO_BUTTON_PROVIDER ->
                object : QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider() {
                    override fun get():
                            List<QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher> {
                        return listOf(
                            QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
                                factory = CustomChipPickerItemViewHolderFactory,
                                matches = CustomChipPickerItemViewHolderFactory::matcher
                            )
                        )
                    }
                }
            else -> throw NotImplementedError()
        }
}