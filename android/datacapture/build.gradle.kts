
plugins {
  id("com.android.library")
  id("kotlin-android")
  id("maven-publish")
  jacoco
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

android {
  namespace = "com.google.android.fhir.datacapture"
  compileSdk = 34

  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.datacapture"
  }

  buildFeatures { viewBinding = true }

  buildTypes {

    create("debugNonProxy") {
      initWith(getByName("debug"))
      buildConfigField(
        "boolean",
        "IS_NON_PROXY_APK",
        "true",
      )
    }

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

  packaging {
    resources.excludes.addAll(
      listOf("META-INF/ASL2.0", "META-INF/ASL-2.0.txt", "META-INF/LGPL-3.0.txt"),
    )
  }

//  packagingOptions {
//    resources.excludes.addAll(
//      listOf("META-INF/ASL2.0", "META-INF/ASL-2.0.txt", "META-INF/LGPL-3.0.txt")
//    )
//  }

  kotlinOptions { jvmTarget = JavaVersion.VERSION_1_8.toString() }
  sourceSets { getByName("androidTest").apply { resources.setSrcDirs(listOf("sampledata")) } }

  testOptions { animationsDisabled = true }
}

configurations { all { exclude(module = "xpp3") } }

dependencies {
  api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.8.0")

  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

  implementation("com.google.android.fhir:common:0.1.0-alpha05")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.core:core-ktx:1.10.1")
  implementation("androidx.fragment:fragment-ktx:1.6.0")
  implementation("com.github.bumptech.glide:glide:4.14.2")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-guava:6.8.0")
  implementation("ca.uhn.hapi.fhir:hapi-fhir-validation:6.8.0") {
    exclude(module = "commons-logging")
    exclude(module = "httpclient")
//    exclude(group = "net.sf.saxon", module = "Saxon-HE")
  }
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
  implementation("com.google.android.material:material:1.9.0")
//  implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
  implementation("com.jakewharton.timber:timber:5.0.1")
}