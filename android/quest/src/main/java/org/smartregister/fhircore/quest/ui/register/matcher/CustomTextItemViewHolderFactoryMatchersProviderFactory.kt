package org.smartregister.fhircore.quest.ui.register.matcher

import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.QuestionnaireItemViewHolderFactoryMatchersProviderFactory
import org.smartregister.fhircore.quest.ui.register.customui.CustomEditTextStringViewHolderFactory

object CustomTextItemViewHolderFactoryMatchersProviderFactory : QuestionnaireItemViewHolderFactoryMatchersProviderFactory {

    const val TEXT_PROVIDER = "STRING"

    override fun get(
        provider: String,
    ): QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider =
        when (provider) {
            TEXT_PROVIDER ->
                object : QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatchersProvider() {
                    override fun get():
                            List<QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher> {
                        return listOf(
                            QuestionnaireFragment.QuestionnaireItemViewHolderFactoryMatcher(
                                factory = CustomEditTextStringViewHolderFactory,
                                matches = CustomEditTextStringViewHolderFactory::matcher
                            )
                        )
                    }
                }
            else -> throw NotImplementedError()
        }
}