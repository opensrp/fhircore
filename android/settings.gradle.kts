pluginManagement {
    repositories {
        google()
        maven(url = "https://plugins.gradle.org/m2/")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "fhircore-android"
include(":engine")

include(":quest")

include(":dataclerk")
