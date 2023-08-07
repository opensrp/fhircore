import java.net.URL

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("maven-publish")
  jacoco
//  id(Plugins.BuildPlugins.dokka).version(Plugins.Versions.dokka)
}

publishing {
  repositories {
    maven {
      credentials(PasswordCredentials::class)
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      name = "sonatype"
    }
  }
}

//publishArtifact(Releases.DataCapture)

//createJacocoTestReportTask()

android {
  compileSdk = 31

  defaultConfig {
    minSdk = 24
    targetSdk = 31
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.datacapture"
  }

  buildFeatures { viewBinding = true }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }
  compileOptions {
    // Flag to enable support for the new language APIs
    // See https://developer.android.com/studio/write/java8-support
    isCoreLibraryDesugaringEnabled = true

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  packagingOptions {
    resources.excludes.addAll(
      listOf("META-INF/ASL2.0", "META-INF/ASL-2.0.txt", "META-INF/LGPL-3.0.txt")
    )
  }

  kotlinOptions { jvmTarget = JavaVersion.VERSION_1_8.toString() }
//  configureJacocoTestOptions()

  sourceSets { getByName("androidTest").apply { resources.setSrcDirs(listOf("sampledata")) } }

  testOptions { animationsDisabled = true }
}

//afterEvaluate { configureFirebaseTestLab() }

configurations { all { exclude(module = "xpp3") } }

dependencies {
//  androidTestImplementation(Dependencies.AndroidxTest.core)
//  androidTestImplementation(Dependencies.AndroidxTest.extJunit)
//  androidTestImplementation(Dependencies.AndroidxTest.extJunitKtx)
//  androidTestImplementation(Dependencies.AndroidxTest.rules)
//  androidTestImplementation(Dependencies.AndroidxTest.runner)
//  androidTestImplementation(Dependencies.junit)
//  androidTestImplementation(Dependencies.truth)
//  androidTestImplementation(Dependencies.Espresso.espressoCore)
//  androidTestImplementation(Dependencies.Espresso.espressoContrib) {
//    // build fails with error "Duplicate class found" (org.checkerframework.checker.*)
//    exclude(group = "org.checkerframework", module = "checker")
//  }
  api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.0.1")

  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

  implementation("com.google.android.fhir:common:0.1.0-alpha03")
  implementation("androidx.appcompat:appcompat:1.1.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.1")
  implementation("androidx.core:core-ktx:1.2.0")
  implementation("androidx.fragment:fragment-ktx:1.3.1")
  implementation("com.github.bumptech.glide:glide:4.14.2")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-validation:6.0.1") {
    exclude(module = "commons-logging")
    exclude(module = "httpclient")
    exclude(group = "net.sf.saxon", module = "Saxon-HE")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
  implementation("com.google.android.material:material:1.6.0")
  implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
  implementation("com.jakewharton.timber:timber:5.0.1")

//  testImplementation(Dependencies.AndroidxTest.core)
//  testImplementation(Dependencies.AndroidxTest.fragmentTesting)
//  testImplementation(Dependencies.Kotlin.kotlinCoroutinesTest)
//  testImplementation(Dependencies.Kotlin.kotlinTestJunit)
//  testImplementation(Dependencies.junit)
//  testImplementation(Dependencies.mockitoInline)
//  testImplementation(Dependencies.mockitoKotlin)
//  testImplementation(Dependencies.robolectric)
//  testImplementation(Dependencies.truth)
//  testImplementation(project(":testing"))
}

//tasks.dokkaHtml.configure {
//  outputDirectory.set(
//    file("../docs/${Releases.DataCapture.artifactId}/${Releases.DataCapture.version}")
//  )
//  suppressInheritedMembers.set(true)
//  dokkaSourceSets {
//    named("main") {
//      moduleName.set(Releases.DataCapture.artifactId)
//      moduleVersion.set(Releases.DataCapture.version)
//      noAndroidSdkLink.set(false)
//      sourceLink {
//        localDirectory.set(file("src/main/java"))
//        remoteUrl.set(
//          URL("https://github.com/google/android-fhir/tree/master/datacapture/src/main/java")
//        )
//        remoteLineSuffix.set("#L")
//      }
//      externalDocumentationLink {
//        url.set(URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-r4/"))
//        packageListUrl.set(
//          URL("https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-structures-r4/element-list")
//        )
//      }
//    }
//  }
//}
