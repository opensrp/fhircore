package org.smartregister.fhircore.quest.util

import org.apache.commons.text.StringSubstitutor
import java.util.ResourceBundle
import java.util.Locale
import java.util.MissingResourceException

object LocaleUtil {
    @Throws(IllegalArgumentException::class)
    private fun getBundleStringSubstitutor(resourceBundle: ResourceBundle): StringSubstitutor {
        val lookup = mutableMapOf<String, Any>()
        resourceBundle.keys.toList().forEach { lookup[it] = resourceBundle.getObject(it) }
        return StringSubstitutor(lookup, "{{", "}}")
    }

    fun parseTemplate(bundleName: String, locale: Locale, template: String): String {
        return try {
            val bundle = ResourceBundle.getBundle(bundleName, locale)
            getBundleStringSubstitutor(bundle).replace(template.replace("\\s+".toRegex(), ""))
        } catch (exception:  MissingResourceException){
            template
        }
    }

    fun getBundleNameFromFileSource(fileSource: String) = fileSource.run {
        substring(lastIndexOf('/') + 1, lastIndexOf('.'))
    }
}