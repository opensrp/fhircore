package org.smartregister.fhircore.engine.ui.base

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import com.google.android.fhir.FhirEngine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.configuration.app.ApplicationConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceDataSource
import org.smartregister.fhircore.engine.data.remote.fhir.resource.FhirResourceService
import org.smartregister.fhircore.engine.domain.model.Language
import org.smartregister.fhircore.engine.robolectric.RobolectricTest
import org.smartregister.fhircore.engine.rule.CoroutineTestRule
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferenceKey
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.engine.util.extension.fetchLanguages
import java.util.Locale
import javax.inject.Inject

@HiltAndroidTest
class LanguageSelectorTest : RobolectricTest() {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

//    @get:Rule(order = 1) val coroutineRule = CoroutineTestRule()

    @Inject
    lateinit var fhirEngine: FhirEngine

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    @Inject
    lateinit var configService: ConfigService

    @Inject
    lateinit var json: Json

    @Inject
    lateinit var fhirContext: FhirContext

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fhirResourceService = mockk<FhirResourceService>()
    private lateinit var fhirResourceDataSource: FhirResourceDataSource
    private lateinit var configRegistry: ConfigurationRegistry

    // System under test
    private lateinit var languageSelector: LanguageSelector

    @Before
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun setUp() {
        hiltRule.inject()
        fhirResourceDataSource = spyk(FhirResourceDataSource(fhirResourceService))
        configRegistry =
            ConfigurationRegistry(
                fhirEngine = fhirEngine,
                fhirResourceDataSource = fhirResourceDataSource,
                sharedPreferencesHelper = sharedPreferencesHelper,
                dispatcherProvider = dispatcherProvider,
                configService = configService,
                json = json,
                context = ApplicationProvider.getApplicationContext<HiltTestApplication>(),
            )
        languageSelector = LanguageSelector(configRegistry, sharedPreferencesHelper)

        // Mock SharedPreferences behavior
//        every { context.getSharedPreferences(any(), any()) } returns sharedPreference
//        every { mockSharedPreferences.getString(any(), any()) } returns null
//        every { mockSharedPreferencesHelper.write(any<String>(), any<String>()) } returns Unit
//        every {
//            hint(Configuration::class)
//            mockConfigurationRegistry.retrieveConfiguration<ApplicationConfiguration>(any())
//        } returns sampleApplicationConfiguration
    }

    @Test
    fun `getDefaultLocale should return shared preference locale if it is supported`() {
        // Arrange
        val sharedPrefLocale = "es"
//        every { mockSharedPreferences.getString(any(), any()) } returns sharedPrefLocale
        every { configRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French"))

        // Act
        val result = languageSelector.getDefaultLocale(context)

        // Assert
        assertEquals(sharedPrefLocale, result)
        verify { sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, sharedPrefLocale) }
    }

    @Test
    fun `getDefaultLocale should return device default locale if shared preference locale is not supported`() {
        // Arrange
        val deviceDefaultLocale = Locale("en", "US")
//        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { configRegistry.fetchLanguages() } returns
                listOf(Language("en", "English"),
                    Language("es", "Espanyol"),
                    Language("fr", "French")
                )
        // Act
        val result = languageSelector.getDefaultLocale(context)

        // Assert
        assertEquals(deviceDefaultLocale.toLanguageTag(), result)
        verify { sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, deviceDefaultLocale.toLanguageTag()) }
    }

    @Test
    fun `getDefaultLocale should return app config default locale if device default locale is not supported`() {
        // Arrange
        val appConfigDefaultLocale = "fr"
//        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { configRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French")) as List<Language>

        // Act
        val result = languageSelector.getDefaultLocale(context)

        // Assert
        assertEquals(appConfigDefaultLocale, result)
        verify { sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, appConfigDefaultLocale) }
    }

    @Test
    fun `getDefaultLocale should return first locale in languages list if no other locale is supported`() {
        // Arrange
        val firstLocaleInLanguagesList = "en"
//        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { configRegistry.fetchLanguages() } returns listOf(Language("en", "English"),
            Language("es", "Espanyol"),
            Language("fr", "French"))

        // Act
        val result = languageSelector.getDefaultLocale(context)

        // Assert
        assertEquals(firstLocaleInLanguagesList, result)
        verify { sharedPreferencesHelper.write(SharedPreferenceKey.LANG.name, firstLocaleInLanguagesList) }
    }
}