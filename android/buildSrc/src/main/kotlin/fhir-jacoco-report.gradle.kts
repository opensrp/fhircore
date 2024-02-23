open class FhirJacocoReportTask : JacocoReport() {
    @TaskAction
    fun setUpJacoccReport(){
        dependsOn(listOf(
            "testDebugUnitTest",
//       Uncomment to include instrumentation tests coverage
//       "createDebugCoverageReport"
        ))

        reports {
            html.required.set(true)
            xml.required.set(true)
        }

        val excludes = listOf(
            // Android
            "**Constant*.*",
            "**/*\$ViewBinder*.*",
            "android/**/*.*",
            "**/*Test*.*",
            "**/*\$ViewInjector*.*",
            "**/BuildConfig.*",
            "**/*BR*.*",
            "**/Manifest*.*",
            "**/R.class",
            "**/R$*.class",
            // Kotlin (Sealed and Data classes)
            "**/*\$Lambda$*.*",
            "**/*Companion*.*",
            "**/*\$Result.*",
            "**/*\$Result$*.*",
            // Data classes and enums without functions or methods
            "**/org/smartregister/fhircore/engine/auth/*",
            "**/org/smartregister/fhircore/engine/configuration/ConfigType*.*",
            "**/org/smartregister/fhircore/engine/configuration/app/*",
            "**/org/smartregister/fhircore/engine/configuration/geowidget/*",
            "**/org/smartregister/fhircore/engine/configuration/navigation/*",
            "**/org/smartregister/fhircore/engine/configuration/profile/*",
            "**/org/smartregister/fhircore/engine/configuration/register/*",
            "**/org/smartregister/fhircore/engine/configuration/report/measure/*",
            "**/org/smartregister/fhircore/engine/configuration/view/CardViewProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/ColumnProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/CompoundTextProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/ListProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/PersonalDataProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/RowProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/ServiceCardProperties*.*",
            "**/org/smartregister/fhircore/engine/configuration/view/SpacerProperties*.*",
            "**/org/smartregister/fhircore/engine/data/remote/model/response/*",
            "**/org/smartregister/fhircore/engine/domain/model/CarePlanConfig*.*",
            "**/org/smartregister/fhircore/engine/domain/model/ExtractedResource*.*",
            "**/org/smartregister/fhircore/engine/domain/model/FhirResourceConfigs*.*",
            "**/org/smartregister/fhircore/engine/domain/model/OverflowMenuItemConfig*.*",
            "**/org/smartregister/fhircore/engine/domain/model/RepositoryResourceData*.*",
            "**/org/smartregister/fhircore/engine/domain/model/RuleConfig*.*",
            "**/org/smartregister/fhircore/engine/domain/model/SearchFilters*.*",
            "**/org/smartregister/fhircore/engine/domain/model/ServiceMemberIcon*.*",
            "**/org/smartregister/fhircore/engine/domain/model/ServiceStatus*.*",
            "**/org/smartregister/fhircore/engine/domain/model/SnackBarMessageConfig*.*",
            "**/org/smartregister/fhircore/engine/domain/model/TopBarConfig*.*",
            "**/org/smartregister/fhircore/engine/data/remote/model/response/*",
            "**/org/smartregister/fhircore/geowidget/model/*",
            // DI (Dagger and Hilt)
            "**/*_MembersInjector.class",
            "**/Dagger*Component.class",
            "**/Dagger*Component\$Builder.class",
            "**/Dagger*Subcomponent*.class",
            "**/*Subcomponent\$Builder.class",
            "**/*Module_*Factory.class",
            "**/*_Factory*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*",
            "**/dagger/hilt/internal/*",
            "**/hilt_aggregated_deps/*",
            "**/di/*",
            "**/*Hilt*.*",
            // Data Binding
            "**/databinding/*",
            "org/hl7/fhir/*"
        )

        val javaDebugTree =
            project.fileTree("${project.buildDir}/intermediates/javac/debug/classes/")
                .exclude(excludes)
        val kotlinDebugTree =
            project.fileTree("${project.buildDir}/tmp/kotlin-classes/debug").exclude(excludes)
        val mainSrc = "${project.projectDir}/src/main/java"
        val kotlinSrc = "${project.projectDir}/src/main/kotlin"

        sourceDirectories.setFrom(project.files(listOf(mainSrc, kotlinSrc)))
        classDirectories.setFrom(project.files(listOf(javaDebugTree, kotlinDebugTree)))

        executionData.setFrom(
            project.fileTree(project.buildDir){
                include(
                    listOf(
                        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                        // Uncomment to include instrumentation test coverage
                        // "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
                    )
                )
            },
            "${project.projectDir}/coverage.ec"
        )
    }

}

project.tasks.register<FhirJacocoReportTask>(name = "jacocoTestReport")
