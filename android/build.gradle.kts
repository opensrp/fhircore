// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.diffplug.gradle.spotless.SpotlessExtension
import groovy.lang.Closure
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath(Deps.kotlin_coveralls_plugin)
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath(Deps.dokka_plugin)
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("com.github.kt3k.coveralls") version "2.12.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10"
    id("com.google.dagger.hilt.android") version Deps.versions.hiltVersion apply false
    id("androidx.navigation.safeargs.kotlin") version "2.5.3" apply false
    id("org.jetbrains.dokka") version "1.8.20"
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("de.mannodermaus.android-junit5") version "1.9.3.0" apply false
    id("com.google.gms.google-services") version "4.3.14" apply false
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
    id("com.google.firebase.crashlytics") version "2.9.5"
    id("com.google.firebase.appdistribution") version "5.0.0" apply false
}

allprojects {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://jcenter.bintray.com/")
        ProjectProperties.readProperties("local.properties")
            .takeIf { it.getProperty("dtreeRepositoryUsername") != null && it.getProperty("dtreeRepositoryPassword") != null }
            ?.let {
                maven {
                    url = uri("https://maven.pkg.github.com/d-tree-org/android-fhir")
                    name = "dtreeRepository"
                    credentials {
                        username = it["dtreeRepositoryUsername"]?.toString()
                        password = it["dtreeRepositoryPassword"]?.toString()
                    }
                }
            }
        maven {
            name = "fhirsdk"
            url = uri("/Users/ndegwamartin/.m2.dev/fhirsdk")
        }
    }
}

subprojects {
    apply {
        plugin("com.diffplug.spotless")
        plugin("jacoco")
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

    configurations.configureEach {
        resolutionStrategy {
            eachDependency {
                if ("org.jacoco" == requested.group) {
                    useVersion(Deps.versions.jacoco_tool)
                }
            }
        }
    }

    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*", "**org.hl7*")
        }

        beforeTest(object : Closure<TestDescriptor>(null) {
            fun doCall(args: TestDescriptor) {
                println("${args.className} > ${args.name} STARTED")
            }
        })
        testLogging {
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            debug {
                events = setOf(TestLogEvent.STARTED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
            }
            info {
                events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            }
        }
        minHeapSize = "4608m"
        maxHeapSize = "4608m"
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }
}
