import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration


// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  dependencies {
    classpath("de.mannodermaus.gradle.plugins:android-junit5:1.8.2.1")
    classpath("com.android.tools.build:gradle:7.1.3")
    classpath("org.jetbrains.dokka:dokka-base:1.8.20")
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id("com.github.kt3k.coveralls") version "2.12.0"
  id("org.jetbrains.kotlin.jvm") version "1.8.10"
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.dagger.hilt.android) apply false
  alias(libs.plugins.org.jetbrains.dokka)
  alias(libs.plugins.org.owasp.dependencycheck)
  alias(libs.plugins.com.diffplug.spotless)
  alias(libs.plugins.androidx.navigation.safeargs) apply false
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
    val lintVersion = "0.49.0"

    kotlin {
      target("**/*.kt")
      ktlint(lintVersion)
      ktfmt().googleStyle()
      licenseHeaderFile("${project.rootProject.projectDir}/license-header.txt")
    }

    kotlinGradle {
      target("*.gradle.kts")
      ktlint(lintVersion)
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
          "org.jacoco" -> useVersion("0.8.7")
        }
      }
    }
  }

  tasks.withType<Test> {
    configure<JacocoTaskExtension> {
      isIncludeNoLocationClasses = true
      excludes = listOf("jdk.internal.*")
    }
  }
}
