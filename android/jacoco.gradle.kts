import org.gradle.testing.jacoco.tasks.JacocoReport

val module = if (project.name == "quest") "Quest" else ""

tasks.create(name = "fhircoreJacocoReport", type = JacocoReport::class) {
  dependsOn(
    setOf(
      "test${module}DebugUnitTest", // Generates unit test coverage report
      "connected${module}DebugAndroidTest" // Generates instrumentation test coverage report
    )
  )
  reports {
    xml.required.set(true)
    html.required.set(true)
  }

  val excludes =
    listOf(
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
      "**/org/smartregister/fhircore/engine/configuration/*",
      "**/org/smartregister/fhircore/engine/data/remote/model/*",
      "**/org/smartregister/fhircore/engine/domain/model/*",
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

  val moduleVariant = if (project.name == "quest") "questDebug" else "debug"
  val javaDebugTree =
    fileTree(baseDir = "${project.buildDir}/intermediates/javac/${moduleVariant}/classes/")
      .exclude(excludes)
  val kotlinDebugTree =
    fileTree(baseDir = "${project.buildDir}/tmp/kotlin-classes/${moduleVariant}").exclude(excludes)
  val mainSrc = "${project.projectDir}/src/main/java"
  val kotlinSrc = "${project.projectDir}/src/main/kotlin"

  sourceDirectories.setFrom(files(listOf(mainSrc, kotlinSrc)))
  classDirectories.setFrom(files(listOf(javaDebugTree, kotlinDebugTree)))

  executionData.setFrom(
    fileTree(baseDir = project.buildDir) {
      include(
        listOf(
          "outputs/unit_test_code_coverage/${moduleVariant}UnitTest/test${module}DebugUnitTest.exec",
          "outputs/code_coverage/${moduleVariant}AndroidTest/connected/**/*.ec"
        )
      )
    }
  )
}
