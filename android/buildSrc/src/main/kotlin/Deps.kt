// Common gradle configuration file for dependencies.
// This makes sure all modules use the same dependencies/versions.
// This file is referenced by the project-level build.gradle file.
// Entries in each section of this file should be sorted alphabetically.

object Deps {
    object sdk_versions {
        const val compile_sdk = 34
        const val min_sdk = 26
        const val target_sdk = 34
    }

    const val build_tool_version = "30.0.3"

    object versions {
        const val activity = "1.2.1"
        const val android_gradle_plugin = "7.1.2"
        const val appcompat = "1.4.1"
        const val atsl_core = "1.5.0"
        const val atsl_expresso = "3.5.1"
        const val atsl_junit = "1.1.5"
        const val atsl_rules = "1.5.0"
        const val atsl_runner = "1.5.2"
        const val caffeine = "2.9.0"
        const val constraint_layout = "1.1.3"
        const val coroutines = "1.8.1"
        const val core = "1.7.0"
        const val cql_engine = "1.3.14-SNAPSHOT"
        const val desugar = "2.0.4"
        const val fragment = "1.4.1"
        const val fhir_protos = "0.6.1"
        const val guava = "28.2-android"
        const val hapi_r4 = "5.3.0"
        const val junit5_api = "5.9.3"
        const val kotlin = "1.9.24"
        const val lifecycle = "2.2.0"
        const val material = "1.5.0"
        const val okhttp_logging_interceptor = "4.0.0"
        const val recyclerview = "1.1.0"
        const val retrofit = "2.7.2"
        const val robolectric = "4.9.2"
        const val room = "2.4.2"
        const val spotless = "6.25.0"
        const val truth = "1.0.1"
        const val work = "2.9.0"
        const val json_tools = "1.13"
        const val kotlin_coveralls = "2.12.2"
        const val jacoco_tool = "0.8.11"
        const val ktlint = "0.41.0"
        const val joda_time = "2.10.5"
        const val timber = "4.7.1"
        const val mockk = "1.13.5"
        const val dokka = "1.8.20"
        const val androidx_test = "2.2.0"
        const val accompanist_swiperefresh = "0.26.4-beta"
        const val compose = "1.4.3"
        const val hiltVersion = "2.51"
        const val hiltWorkerVersion ="1.2.0"
    }

    const val activity = "androidx.activity:activity:${versions.activity}"
    const val android_gradle_plugin = "com.android.tools.build:gradle:${versions.android_gradle_plugin}"
    const val appcompat = "androidx.appcompat:appcompat:${versions.appcompat}"
    const val kotlin_coveralls_plugin = "gradle.plugin.org.kt3k.gradle.plugin:coveralls-gradle-plugin:${versions.kotlin_coveralls}"

    object atsl {
        const val core = "androidx.test:core-ktx:${versions.atsl_core}"
        const val espresso = "androidx.test.espresso:espresso-core:${versions.atsl_expresso}"
        const val ext_junit = "androidx.test.ext:junit:${versions.atsl_junit}"
        const val ext_junit_ktx = "androidx.test.ext:junit-ktx:${versions.atsl_junit}"
        const val rules = "androidx.test:rules:${versions.atsl_rules}"
        const val runner = "androidx.test:runner:${versions.atsl_runner}"
    }
    const val caffeine = "com.github.ben-manes.caffeine:caffeine:${versions.caffeine}"
    const val constraint_layout = "androidx.constraintlayout:constraintlayout:${versions.constraint_layout}"
    const val core = "androidx.core:core-ktx:${versions.core}"

    object coroutines {
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${versions.coroutines}"
    }

    object cql_engine{
        const val core = "org.opencds.cqf:cql-engine:${versions.cql_engine}"
        const val fhir = "org.opencds.cqf:cql-engine-fhir:${versions.cql_engine}"
    }

    const val desugar = "com.android.tools:desugar_jdk_libs:${versions.desugar}"
    const val fragment = "androidx.fragment:fragment-ktx:${versions.fragment}"
    const val fragment_testing = "androidx.fragment:fragment-testing:${versions.fragment}"
    const val fhir_protos = "com.google.fhir:r4:${versions.fhir_protos}"
    const val guava = "com.google.guava:guava:${versions.guava}"
    const val hapi_r4 = "ca.uhn.hapi.fhir:hapi-fhir-structures-r4:${versions.hapi_r4}"
    const val junit5_api = "org.junit.jupiter:junit-jupiter-api:${versions.junit5_api}"
    const val junit5_engine = "org.junit.jupiter:junit-jupiter-engine:${versions.junit5_api}"
    const val junit5_engine_vintage = "org.junit.vintage:junit-vintage-engine:${versions.junit5_api}"
    const val mockk = "io.mockk:mockk:${versions.mockk}"
    const val mockk_android = "io.mockk:mockk-android:${versions.mockk}"
    const val dokka_plugin = "org.jetbrains.dokka:dokka-gradle-plugin:${versions.dokka}"

    object kotlin{
        const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${versions.kotlin}"
        const val test = "org.jetbrains.kotlin:kotlin-test-junit:${versions.kotlin}"
    }

    object lifecycle{
        const val extensions = "androidx.lifecycle:lifecycle-extensions:${versions.lifecycle}"
        const val livedata_core_ktx = "androidx.lifecycle:lifecycle-livedata-core-ktx:${versions.lifecycle}"
        const val livedata_ktx = "androidx.lifecycle:lifecycle-livedata-ktx:${versions.lifecycle}"
        const val runtime = "androidx.lifecycle:lifecycle-runtime:${versions.lifecycle}"
        const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.lifecycle}"
        const val viewmodel_ktx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.lifecycle}"
    }

    const val material = "com.google.android.material:material:${versions.material}"
    const val okhttp_logging_interceptor = "com.squareup.okhttp3:logging-interceptor:${versions.okhttp_logging_interceptor}"
    const val recyclerview = "androidx.recyclerview:recyclerview:${versions.recyclerview}"

    object retrofit{
        const val core = "com.squareup.retrofit2:retrofit:${versions.retrofit}"
        const val gson = "com.squareup.retrofit2:converter-gson:${versions.retrofit}"
        const val mock = "com.squareup.retrofit2:retrofit-mock:${versions.retrofit}"
    }

    const val robolectric = "org.robolectric:robolectric:${versions.robolectric}"

    object room {
        const val compiler = "androidx.room:room-compiler:${versions.room}"
        const val ktx = "androidx.room:room-ktx:${versions.room}"
        const val runtime = "androidx.room:room-runtime:${versions.room}"
    }

    const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:${versions.spotless}"
    const val truth = "com.google.truth:truth:${versions.truth}"

    object work{
        const val runtime = "androidx.work:work-runtime-ktx:${versions.work}"
    }

    object json_tools{
        const val json_patch = "com.github.java-json-tools:json-patch:${versions.json_tools}"
    }

    const val joda_time = "joda-time:joda-time:${versions.joda_time}"
    const val timber = "com.jakewharton.timber:timber:${versions.timber}"

    object androidx{
        const val core_test = "androidx.arch.core:core-testing:${versions.androidx_test}"
    }

    object accompanist{
        const val swiperefresh = "com.google.accompanist:accompanist-swiperefresh:${versions.accompanist_swiperefresh}"
    }
}