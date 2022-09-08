package org.smartregister.fhircore.quest.ui.register

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.ConfigurationRegistry
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.robolectric.RobolectricTest
import javax.inject.Inject

@HiltAndroidTest
class RegisterViewModelTest : RobolectricTest() {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    private lateinit var registerViewModel: RegisterViewModel
    lateinit var registerRepository: RegisterRepository

    @Inject
    lateinit var configurationRegistry: ConfigurationRegistry
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private lateinit var registerViewModelMock: RegisterViewModel


    @Before
    fun setUp() {
        hiltRule.inject()
        registerRepository = mockk()
        sharedPreferencesHelper = mockk()
        registerViewModelMock = mockk()
        registerViewModel = RegisterViewModel(
            registerRepository = registerRepository,
            configurationRegistry = configurationRegistry,
            sharedPreferencesHelper = sharedPreferencesHelper
        )
    }

    @Test
    fun testPaginateRegisterData() {
        val registerId = "12727277171"
        every { registerViewModelMock.paginateRegisterData(any(), any()) } just runs
        registerViewModelMock.paginateRegisterData(registerId, false)
        verify { registerViewModelMock.paginateRegisterData(registerId, false) }

    }
}