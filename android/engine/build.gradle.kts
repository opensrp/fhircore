import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id ("com.android.library")
    id ("kotlin-android")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
    id ("org.jetbrains.kotlin.plugin.serialization")
    id ("de.mannodermaus.android-junit5")
    id ("jacoco")
    id ("dagger.hilt.android.plugin")
    id ("androidx.navigation.safeargs")
}

apply(from = "../jacoco.gradle")

android {

    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
        testInstrumentationRunner = "org.smartregister.fhircore.engine.EngineTestRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
      getByName("debug") {
        isTestCoverageEnabled = true
      }

      getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0"
    }

    packagingOptions {
       excludes.add("license.html")
       excludes.add("readme.html")
       excludes.add("META-INF/DEPENDENCIES")
       excludes.add("META-INF/LICENSE")
       excludes.add("META-INF/LICENSE.txt")
       excludes.add("META-INF/license.txt")
       excludes.add("META-INF/license.html")
       excludes.add("META-INF/LICENSE.md")
       excludes.add("META-INF/NOTICE")
       excludes.add("META-INF/NOTICE.txt")
       excludes.add("META-INF/NOTICE.md")
       excludes.add("META-INF/notice.txt")
       excludes.add("META-INF/ASL2.0")
       excludes.add("META-INF/ASL-2.0.txt")
       excludes.add("META-INF/LGPL-3.0.txt")
       excludes.add("META-INF/sun-jaxb.episode")
       excludes.add("META-INF/*.kotlin_module")
       excludes.add("META-INF/INDEX.LIST")
    }

    testOptions {
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true

        }
    }

  testCoverage { jacocoVersion = "0.8.7" }
}

tasks.withType<Test>() {
  testLogging {
    events = setOf(TestLogEvent.SKIPPED)
  }
  jvmArgs("-Xms4608m -Xmx4608m -ea -noverify")
}


configurations {
    all { exclude(group = "org.eclipse.persistence") }
   all { exclude(group = "javax.activation", module ="activation")}
   all { exclude(group = "javax", module ="javaee-api")}
   all { exclude(group = "xml-apis")}
   all { exclude(group = "xpp3") }
}

dependencies {
    coreLibraryDesugaring(libs.core.desugar)

    //Library dependencies
    implementation(libs.preference.ktx)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.constraintlayout.compose)
    implementation(libs.fragment.ktx)
    implementation(libs.security.crypto)
    implementation(libs.cardview)
    implementation(libs.joda.time)
    implementation(libs.paging.runtime.ktx)
    implementation(libs.compressor)
    implementation(libs.xercesImpl)
    implementation(libs.msg.simple)
    implementation(libs.cqf.cql.engine)
    implementation(libs.cql.engine.jackson)
    implementation(libs.dagger.hilt.android)
    implementation(libs.hilt.work)
    implementation(libs.slf4j.nop)
    implementation(libs.cqf.cql.evaluator) {
       exclude(group ="com.github.ben-manes.caffeine")
       exclude(group ="ca.uhn.hapi.fhir")
    }
    implementation(libs.cql.evaluator.builder) {
       exclude(group ="com.github.ben-manes.caffeine")
       exclude(group ="ca.uhn.hapi.fhir")
    }
    implementation(libs.cql.evaluator.plandefinition) {
       exclude(group ="com.github.ben-manes.caffeine")
       exclude(group ="ca.uhn.hapi.fhir")
    }
    implementation(libs.cql.evaluator.dagger) {
       exclude(group ="com.github.ben-manes.caffeine")
       exclude(group ="ca.uhn.hapi.fhir")
    }

    //Shared dependencies
    api(libs.glide)
    api(libs.p2p.lib)
    api(libs.jjwt)
    api(libs.fhir.common.utils)
    api(libs.lifecycle.livedata.ktx)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.ui)
    api(libs.ui.tooling)
    api(libs.runtime.livedata)
    api(libs.navigation.fragment.ktx)
    api(libs.navigation.ui.ktx)
    api(libs.navigation.compose)
    api(libs.navigation.testing)
    api(libs.material.icons.extended)
    api(libs.material.icons.core)
    api(libs.material3)
    api(libs.foundation)
    api(libs.hilt.navigation.compose)
    api(libs.lifecycle.viewmodel.compose)
    api(libs.paging.compose)
    api(libs.activity.compose)
    api(libs.kotlinx.serialization.json)
    api(libs.accompanist.flowlayout)
    api(libs.accompanist.placeholder)
    api(libs.work.runtime.ktx)
    api(libs.prettytime)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlin.reflect)
    api(libs.stax.api)
    api(libs.caffeine)
    api(libs.gson)
    api(libs.timber)
    api(libs.retrofit)
    api(libs.converter.gson)
    api(libs.retrofit.mock)
    api(libs.retrofit2.kotlinx.serialization.converter)
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)
    api(libs.easy.rules.jexl) {
       exclude(group ="commons-logging", module ="commons-logging")
    }
    api(libs.data.capture) {
        isTransitive= true
       exclude(group ="ca.uhn.hapi.fhir")
       exclude(group ="com.google.android.fhir", module ="engine")
    }
    api(libs.workflow) {
        isTransitive= true
       exclude(group ="xerces")
       exclude(group ="com.github.java-json-tools")
       exclude(group ="org.codehaus.woodstox")
       exclude(group ="ca.uhn.hapi.fhir")
       exclude(group ="com.google.android.fhir", module ="common")
       exclude(group ="com.google.android.fhir", module ="engine")
       exclude(group ="com.github.ben-manes.caffeine")
    }
    api(libs.contrib.barcode) {
        isTransitive= true
       exclude(group ="org.smartregister", module ="data-capture")
       exclude(group ="ca.uhn.hapi.fhir")
       exclude(group ="com.google.android.fhir", module ="common")
       exclude(group ="com.google.android.fhir", module ="engine")
    }
    api(libs.fhir.engine) {
        isTransitive= true
       exclude(group ="com.google.android.fhir", module ="common")
       exclude(group ="com.github.ben-manes.caffeine")
    }

    //Annotation processors
    kapt(libs.hilt.compiler)
    kapt(libs.dagger.hilt.compiler)

    //Annotation processors for test
    kaptTest(libs.hilt.android.compiler)
    kaptAndroidTest(libs.hilt.android.compiler)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    //Test dependencies
    testImplementation(libs.work.runtime.ktx)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.junit.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.core.testing)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.json)
    testImplementation(libs.kotlinx.coroutines.debug)
    testImplementation(libs.navigation.testing)
    testImplementation(libs.work.testing)

    //To run only on debug builds
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.fragment.testing)

    //Android test dependencies
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
}
