import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.smartregister.fhircore.engine.data.local.DefaultRepository
import org.smartregister.fhircore.engine.rulesengine.ResourceDataRulesExecutor
import org.smartregister.fhircore.engine.util.DispatcherProvider
import org.smartregister.fhircore.engine.util.SharedPreferencesHelper
import org.smartregister.fhircore.geowidget.model.Coordinates
import org.smartregister.fhircore.geowidget.model.Feature
import org.smartregister.fhircore.geowidget.model.Geometry
import org.smartregister.fhircore.geowidget.model.ServicePointType
import org.smartregister.fhircore.geowidget.screens.GeoWidgetViewModel
import org.smartregister.fhircore.geowidget.util.extensions.getGeoJsonGeometry
import org.smartregister.fhircore.geowidget.util.extensions.getProperties
import org.smartregister.fhircore.quest.ui.launcher.GeoWidgetLauncherViewModel

@ExperimentalCoroutinesApi
class GeoWidgetLauncherViewModelTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private lateinit var viewModel: GeoWidgetLauncherViewModel
    private lateinit var defaultRepository: DefaultRepository
    private lateinit var mockDispatcherProvider: DispatcherProvider
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    private lateinit var resourceDataRulesExecutor: ResourceDataRulesExecutor

    @Before
    fun setUp() {
        defaultRepository = mockk()
        mockDispatcherProvider = mockk()
        sharedPreferencesHelper = mockk()
        resourceDataRulesExecutor = mockk()
        viewModel = GeoWidgetLauncherViewModel(
            defaultRepository,
            mockDispatcherProvider,
            sharedPreferencesHelper,
            resourceDataRulesExecutor
        )
    }


    // addLocationsToMap method adds locations to the map
    @Test
    fun test_addLocationsToMap() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val locations = setOf(
            Feature(id = "1", type = "Point", geometry = getDefaultGeometry()),
            Feature(id = "2", type = "Point", geometry = getDefaultGeometry())
        )

        // Act
        viewModel.addLocationsToMap(locations)

        // Assert
        assertEquals(2, viewModel.featuresFlow.value.size)
    }

    private fun getDefaultGeometry() = Geometry(coordinates = listOf(Coordinates(3.7, 41.53)))

    // clearLocations method clears all locations from the map
    @Test
    fun test_clearLocations() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val locations = setOf(
            Feature(id = "1", type = "Point"),
            Feature(id = "2", type = "Point")
        )
        viewModel.addLocationsToMap(locations)

        // Act
        viewModel.clearLocations()

        // Assert
        assertEquals(0, viewModel.featuresFlow.value.size)
    }

    // getServicePointKeyToType method returns a map of service point types
    @Test
    fun test_getServicePointKeyToType() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)

        // Act
        val servicePointMap = viewModel.getServicePointKeyToType()

        // Assert
        assertEquals(ServicePointType.EPP, servicePointMap["epp"])
        assertEquals(ServicePointType.CEG, servicePointMap["ceg"])
        assertEquals(ServicePointType.CHRD1, servicePointMap["chrd1"])
        // ... (continue for all service point types)
    }

    // dispatcherProvider provides main, default, io and unconfined dispatchers
    @Test
    fun test_dispatcherProvider() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)

        // Act
        val mainDispatcher = viewModel.dispatcherProvider.main()
        val defaultDispatcher = viewModel.dispatcherProvider.default()
        val ioDispatcher = viewModel.dispatcherProvider.io()
        val unconfinedDispatcher = viewModel.dispatcherProvider.unconfined()

        // Assert
        assertEquals(Dispatchers.Main, mainDispatcher)
        assertEquals(Dispatchers.Default, defaultDispatcher)
        assertEquals(Dispatchers.IO, ioDispatcher)
        assertEquals(Dispatchers.Unconfined, unconfinedDispatcher)
    }

    // GeoWidgetViewModel is properly constructed with a DispatcherProvider
    @Test
    fun test_constructor() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)

        // Assert
        assertNotNull(viewModel.dispatcherProvider)
    }

    // featuresFlow is properly initialized as a StateFlow
    @Test
    fun test_featuresFlowInitialization() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)

        // Assert
        assertNotNull(viewModel.featuresFlow)
    }

    // addLocationsToMap method handles empty set of locations
    @Test
    fun test_addLocationsToMap_emptySet() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val locations = emptySet<Feature>()

        // Act
        viewModel.addLocationsToMap(locations)

        // Assert
        assertEquals(0, viewModel.featuresFlow.value.size)
    }

    // addLocationToMap method handles null geometry
    @Test
    fun test_addLocationToMap_nullGeometry() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val feature = Feature(id = "1", type = "Point", geometry = null)

        // Act
        viewModel.addLocationToMap(feature)

        // Assert
        assertEquals(0, viewModel.featuresFlow.value.size)
    }

    // addLocationToMap method handles null properties
    @Test
    fun test_addLocationToMap_nullProperties() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val feature = Feature(id = "1", type = "Point", properties = emptyMap())

        // Act
        viewModel.addLocationToMap(feature)

        // Assert
        assertEquals(1, viewModel.featuresFlow.value.size)
    }

    // addLocationToMap method handles null coordinates
    @Test
    fun test_addLocationToMap_nullCoordinates() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val feature = Feature(id = "1", type = "Point", geometry = Geometry(null))

        // Act
        viewModel.addLocationToMap(feature)

        // Assert
        assertEquals(0, viewModel.featuresFlow.value.size)
    }

    // getGeoJsonGeometry method handles null geometry
    @Test
    fun test_getGeoJsonGeometry_nullGeometry() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val feature = Feature(id = "1", type = "Point", geometry = null)

        // Act
        val result = feature.getGeoJsonGeometry()

        // Assert
        assertEquals(JSONObject(), result)
    }

    // getProperties method handles null properties
    @Test
    fun test_getProperties_nullProperties() {
        // Arrange
        val viewModel = GeoWidgetViewModel(mockDispatcherProvider)
        val feature = Feature(id = "1", type = "Point", properties = emptyMap())

        // Act
        val result = feature.getProperties()

        // Assert
        assertEquals(JSONObject(), result)
    }


}