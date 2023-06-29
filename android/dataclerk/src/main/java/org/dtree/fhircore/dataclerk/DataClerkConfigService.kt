package org.dtree.fhircore.dataclerk

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ResourceType
import org.smartregister.fhircore.engine.configuration.app.AuthConfiguration
import org.smartregister.fhircore.engine.configuration.app.ConfigService
import org.smartregister.fhircore.engine.sync.ResourceTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataClerkConfigService @Inject constructor(@ApplicationContext val context: Context) :
        ConfigService {

    override fun provideAuthConfiguration() =
            AuthConfiguration(
                    fhirServerBaseUrl = BuildConfig.FHIR_BASE_URL,
                    oauthServerBaseUrl = BuildConfig.OAUTH_BASE_URL,
                    clientId = BuildConfig.OAUTH_CIENT_ID,
                    clientSecret = BuildConfig.OAUTH_CLIENT_SECRET,
                    accountType = BuildConfig.APPLICATION_ID
            )

    override fun defineResourceTags() =
            listOf(
                    ResourceTag(
                            type = ResourceType.CareTeam.name,
                            tag =
                            Coding().apply {
                                system = context.getString(R.string.sync_strategy_careteam_system)
                                display = context.getString(R.string.sync_strategy_careteam_display)
                            }
                    ),
                    ResourceTag(
                            type = ResourceType.Location.name,
                            tag =
                            Coding().apply {
                                system = context.getString(R.string.sync_strategy_location_system)
                                display = context.getString(R.string.sync_strategy_location_display)
                            }
                    ),
                    ResourceTag(
                            type = ResourceType.Organization.name,
                            tag =
                            Coding().apply {
                                system = context.getString(R.string.sync_strategy_organization_system)
                                display = context.getString(R.string.sync_strategy_organization_display)
                            }
                    ),
                    ResourceTag(
                            type = ResourceType.Practitioner.name,
                            tag =
                            Coding().apply {
                                system = context.getString(R.string.sync_strategy_practitioner_system)
                                display = context.getString(R.string.sync_strategy_practitioner_display)
                            }
                    )
            )
}
