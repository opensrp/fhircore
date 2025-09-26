import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.coveralls.gradle.plugin)
    classpath(libs.gradle)
    classpath(libs.dokka.base)
    classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.20-2.0.1")
  }
}

plugins {
  alias(libs.plugins.org.jetbrains.kotlin.jvm)
  alias(libs.plugins.kt3k.coveralls)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.dagger.hilt.android) apply false
  alias(libs.plugins.androidx.navigation.safeargs) apply false
  alias(libs.plugins.org.jetbrains.dokka)
  alias(libs.plugins.org.owasp.dependencycheck)
  alias(libs.plugins.com.diffplug.spotless) apply false
  alias(libs.plugins.android.junit5) apply false
  alias(libs.plugins.org.jetbrains.kotlin.plugin.compose) apply false
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.dokkaHtmlMultiModule {
  dependsOn(":engine:kaptDebugKotlin")
  dependsOn(":engine:kaptDebugNonProxyKotlin")
  dependsOn(":engine:kaptReleaseKotlin")
  moduleName.set("OpenSRP")
  moduleVersion.set(project.version.toString())
  outputDirectory.set(file(layout.buildDirectory.dir("dokka")))
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    footerMessage = "(c) Ona Systems Inc"
  }
}

apply(from = "mapbox.gradle.kts")

allprojects {
  repositories {
    gradlePluginPortal()
    mavenLocal()
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://repository.liferay.com/nexus/content/repositories/public")
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    apply(plugin = "org.owasp.dependencycheck")
    tasks.dependencyCheckAggregate{
      dependencyCheck.formats.add("XML")
    }
  }
}

subprojects {
  apply {
    plugin("com.diffplug.spotless")
    plugin("org.jetbrains.dokka")
    plugin(  "jacoco")
  }

  configure<SpotlessExtension> {

    kotlin {
      target("**/*.kt")
      ktlint(BuildConfigs.ktLintVersion)
      ktfmt().googleStyle()
      licenseHeaderFile("${project.rootProject.projectDir}/license-header.txt")
    }

    kotlinGradle {
      target("*.gradle.kts")
      ktlint(BuildConfigs.ktLintVersion)
      ktfmt().googleStyle()
    }

    format("xml") {
      target("**/*.xml")
      indentWithSpaces()
      trimTrailingWhitespace()
      endWithNewline()
    }

    format("json") {
      target("**/*.json")
      indentWithSpaces(2)
      trimTrailingWhitespace()
    }
  }

  configurations.all {
    resolutionStrategy {
      eachDependency {
        when (requested.group) {
          "org.jacoco" -> useVersion(BuildConfigs.jacocoVersion)
        }
      }
    }
  }

 tasks.withType<Test> {
    configure<JacocoTaskExtension> {
      isIncludeNoLocationClasses = true
      excludes = listOf("jdk.internal.*", "**org.hl7*")
    }
  }
}
