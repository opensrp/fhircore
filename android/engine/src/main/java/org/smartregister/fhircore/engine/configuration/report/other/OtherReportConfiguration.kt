package org.smartregister.fhircore.engine.configuration.report.other

import kotlinx.serialization.Serializable
import org.smartregister.fhircore.engine.configuration.ConfigType
import org.smartregister.fhircore.engine.configuration.Configuration
import org.smartregister.fhircore.engine.configuration.report.measure.ReportConfiguration
import org.smartregister.fhircore.engine.configuration.view.TabViewProperties
import org.smartregister.fhircore.engine.domain.model.ResourceConfig
import org.smartregister.fhircore.engine.domain.model.RuleConfig

@Serializable
data class OtherReportConfiguration(
    override var appId: String,
    override var configType: String = ConfigType.MeasureReport.name,
    val id: String,
    val showDateFilter: Boolean = true,
    val tabBar: TabViewProperties? = null,
    val reports: List<ReportConfiguration> = emptyList(),
    val resources: List<ResourceConfig> = emptyList(),
    val rules: List<RuleConfig> = emptyList(),
) : Configuration()