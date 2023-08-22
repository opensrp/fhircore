package org.smartregister.fhircore.quest.integration.ui.report.measure.components


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.paging.PagingData
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.workflow.FhirOperator
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.data.local.register.RegisterRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DefaultDispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.quest.data.report.measure.MeasureReportRepository
import org.smartregister.fhircore.quest.integration.Faker
import org.smartregister.fhircore.quest.ui.report.measure.MeasureReportViewModel
import org.smartregister.fhircore.quest.ui.report.measure.screens.MeasureReportSubjectsScreen
import org.smartregister.fhircore.quest.ui.shared.models.MeasureReportSubjectViewData
import org.smartregister.fhircore.quest.util.mappers.MeasureReportSubjectViewDataMapper

class MeasureReportSubjectScreenTest {

    @get:Rule(order = 0) val composeTestRule = createComposeRule()
    private lateinit var measureReportViewModel: MeasureReportViewModel
    private val fhirEngine: FhirEngine = mockk()
    private val fhirOperator: FhirOperator = mockk()
    private val sharedPreferencesHelper: SharedPreferencesHelper = mockk(relaxed = true)
    private val dispatcherProvider: DefaultDispatcherProvider = mockk()
    private var measureReportSubjectViewDataMapper: MeasureReportSubjectViewDataMapper =
        mockk(relaxed = true)
    private val configurationRegistry = Faker.buildTestConfigurationRegistry()
    private var registerRepository: RegisterRepository = mockk(relaxed = true)
    private var defaultRepository: DefaultRepository = mockk(relaxed = true)
    private var resourceDataRulesExecutor: ResourceDataRulesExecutor = mockk(relaxed = true)
    private var measureReportRepository: MeasureReportRepository = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxUnitFun = true)
    private val testData = createMockMeasureReportSubjectViewDataList()
    private val expectedPagingData = PagingData.from(testData)
    val mockFlow: Flow<PagingData<MeasureReportSubjectViewData>> = flowOf(expectedPagingData)



    @Before
    fun setup() {
        measureReportViewModel =
            spyk(
                MeasureReportViewModel(
                    fhirEngine = fhirEngine,
                    fhirOperator = fhirOperator,
                    sharedPreferencesHelper = sharedPreferencesHelper,
                    dispatcherProvider = dispatcherProvider,
                    measureReportSubjectViewDataMapper = measureReportSubjectViewDataMapper,
                    configurationRegistry = configurationRegistry,
                    registerRepository = registerRepository,
                    defaultRepository = defaultRepository,
                    resourceDataRulesExecutor = resourceDataRulesExecutor,
                    measureReportRepository = measureReportRepository,
                ),
            )
    }
    @Test
    fun measureReportSubjectsScreenContentDisplayedWithNoDatat() {
        composeTestRule.setContent {
            MeasureReportSubjectsScreen(
                reportId = "supplyChainMeasureReport",
                navController = navController,
                measureReportViewModel = measureReportViewModel
            )
        }
        composeTestRule.onNodeWithText("SELECT SUBJECT").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("CircularProgressBar").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Error Message").assertDoesNotExist()
    }

    private fun createMockMeasureReportSubjectViewDataList(): List<MeasureReportSubjectViewData> {
        return listOf(
            MeasureReportSubjectViewData(ResourceType.Patient, "patientId1", "Alice", "Doe"),
            MeasureReportSubjectViewData(ResourceType.Patient, "patientId2", "Bob", "Smith")
        )
    }
}