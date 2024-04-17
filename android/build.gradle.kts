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

}

tasks.dokkaHtmlMultiModule {
  moduleName.set("OpenSRP")
  moduleVersion.set(project.version.toString())
  outputDirectory.set(file(buildDir.resolve("dokka")))
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    footerMessage = "(c) Ona Systems Inc"
  }
}

allprojects {
  repositories {
    gradlePluginPortal()
    mavenLocal()
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://jcenter.bintray.com/")
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
