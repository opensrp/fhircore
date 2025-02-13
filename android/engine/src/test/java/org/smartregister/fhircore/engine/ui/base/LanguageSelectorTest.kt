package org.smartregister.fhircore.engine.ui.base

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import java.util.Locale

class LanguageSelectorTest {

    // Mock dependencies
    private val mockContext: Context = mockk()
    private val mockSharedPreferences: SharedPreferences = mockk()
    private val mockEditor: SharedPreferences.Editor = mockk()
    private val mockConfigurationRegistry: ConfigurationRegistry = mockk()
    private val mockSharedPreferencesHelper: SharedPreferencesHelper = mockk()
    private val sampleApplicationConfiguration = ApplicationConfiguration(
        appId = "test",
        defaultLocale = "fr",
        languages = listOf("en", "es", "fr"),
    )

    // System under test
    private lateinit var languageSelector: LanguageSelector

    @Before
    fun setUp() {
        // Initialize the LanguageSelector with mocked dependencies
        languageSelector = LanguageSelector(mockConfigurationRegistry, mockSharedPreferencesHelper)

        // Mock SharedPreferences behavior
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockSharedPreferencesHelper.write(any<String>(), any<String>()) } returns Unit
        every { mockConfigurationRegistry.retrieveConfiguration<ApplicationConfiguration>(eq(ConfigType.Application)) } returns sampleApplicationConfiguration

    }

    @Test
    fun `getDefaultLocale should return shared preference locale if it is supported`() {
        // Arrange
        val sharedPrefLocale = "es"
        every { mockSharedPreferences.getString(any(), any()) } returns sharedPrefLocale
        every { mockConfigurationRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French"))

        // Act
        val result = languageSelector.getDefaultLocale(mockContext)

        // Assert
        assertEquals(sharedPrefLocale, result)
        verify { mockSharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, sharedPrefLocale) }
    }

    @Test
    fun `getDefaultLocale should return device default locale if shared preference locale is not supported`() {
        // Arrange
        val deviceDefaultLocale = Locale("en", "US")
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockConfigurationRegistry.fetchLanguages() } returns
                listOf(Language("en", "English"),
                    Language("es", "Espanyol"),
                    Language("fr", "French")
                )
        // Act
        val result = languageSelector.getDefaultLocale(mockContext)

        // Assert
        assertEquals(deviceDefaultLocale.toLanguageTag(), result)
        verify { mockSharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, deviceDefaultLocale.toLanguageTag()) }
    }

    @Test
    fun `getDefaultLocale should return app config default locale if device default locale is not supported`() {
        // Arrange
        val appConfigDefaultLocale = "fr"
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockConfigurationRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French")) as List<Language>

        // Act
        val result = languageSelector.getDefaultLocale(mockContext)

        // Assert
        assertEquals(appConfigDefaultLocale, result)
        verify { mockSharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, appConfigDefaultLocale) }
    }

    @Test
    fun `getDefaultLocale should return first locale in languages list if no other locale is supported`() {
        // Arrange
        val firstLocaleInLanguagesList = "en"
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockConfigurationRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French"))

        // Act
        val result = languageSelector.getDefaultLocale(mockContext)

        // Assert
        assertEquals(firstLocaleInLanguagesList, result)
        verify { mockSharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, firstLocaleInLanguagesList) }
    }
}