// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.diffplug.gradle.spotless.SpotlessExtension
import groovy.lang.Closure
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
        repositories {
            google()
            maven(url = "https://plugins.gradle.org/m2/")
            maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.22")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.45")
        classpath(Deps.spotless)
        classpath(Deps.kotlin_coveralls_plugin)
        classpath(Deps.dokka_plugin)
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.google.gms:google-services:4.3.14")
        classpath("com.google.firebase:perf-plugin:1.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.5")
    }
}

allprojects {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven {
            name = "fhirsdk"
            url = uri("/Users/ndegwamartin/.m2.dev/fhirsdk")
        }
    }
}

subprojects {
    apply {
        plugin("com.diffplug.spotless")
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
