import org.junit.Assert
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.geowidget.GeoWidgetConfiguration
import org.smartregister.fhircore.engine.configuration.QuestionnaireConfig
import org.smartregister.fhircore.engine.domain.model.FhirResourceConfig
import org.smartregister.fhircore.engine.domain.model.ResourceConfig

class GeoWidgetConfigurationTest {
    @Test
    fun testGeoWidgetConfiguration() {
        val appId = "testAppId"
        val id = "testId"
        val profileId = "testProfileId"
        val registrationQuestionnaire = QuestionnaireConfig("testQuestionnaireId")
        val resourceConfig = FhirResourceConfig(baseResource = ResourceConfig("name","resource"))
        val geoWidgetConfiguration = GeoWidgetConfiguration(
                appId, id = id, profileId = profileId,
                registrationQuestionnaire = registrationQuestionnaire, resourceConfig = resourceConfig
        )
        Assert.assertEquals(appId, geoWidgetConfiguration.appId)
        Assert.assertEquals("geoWidget", geoWidgetConfiguration.configType)
        Assert.assertEquals(id, geoWidgetConfiguration.id)
        Assert.assertEquals(profileId, geoWidgetConfiguration.profileId)
        Assert.assertEquals(registrationQuestionnaire, geoWidgetConfiguration.registrationQuestionnaire)
        Assert.assertEquals(resourceConfig, geoWidgetConfiguration.resourceConfig)
    }
}

