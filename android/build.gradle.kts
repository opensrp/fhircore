import com.diffplug.gradle.spotless.SpotlessExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  repositories {
    google()
    maven(url = "https://plugins.gradle.org/m2/")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }

  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    classpath("org.jetbrains.kotlin:kotlin-serialization:1.7.10")
    classpath("com.google.dagger:hilt-android-gradle-plugin:2.42")
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:5.11.0")
    classpath("org.jacoco:org.jacoco.core:0.8.7")
    classpath("gradle.plugin.org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.12.0")
    classpath("de.mannodermaus.gradle.plugins:android-junit5:1.8.2.1")
    classpath("com.android.tools.build:gradle:7.1.3")
  }
}

plugins { id("org.jetbrains.dokka") version "1.7.20" }

allprojects {
  repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://jcenter.bintray.com/")
  }
}

subprojects {
  apply {
    plugin("com.diffplug.spotless")
    plugin("org.jetbrains.dokka")
    plugin(  "jacoco")
  }

  configure<SpotlessExtension> {
    val lintVersion = "0.41.0"
    val lintOptions = mapOf("indent_size" to "2", "continuation_indent_size" to "2")

    kotlin {
      target("**/*.kt")
      ktlint(lintVersion).userData(lintOptions)
      ktfmt().googleStyle()
      licenseHeaderFile("${project.rootProject.projectDir}/license-header.txt")
    }

    kotlinGradle {
      target("*.gradle.kts")
      ktlint(lintVersion).userData(lintOptions)
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

  tasks.dokkaHtml.configure { outputDirectory.set(file("${project.rootProject.projectDir}/docs")) }

  tasks.withType<Test> {
    configure<JacocoTaskExtension> {
      isIncludeNoLocationClasses = true
      excludes = listOf("jdk.internal.*")
    }
  }
}
