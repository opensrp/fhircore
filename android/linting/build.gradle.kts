plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies { implementation(libs.bundles.klint) }

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}
