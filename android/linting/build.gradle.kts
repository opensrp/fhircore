plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies { implementation(libs.bundles.klint) }

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
