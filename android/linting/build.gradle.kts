plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.ktlint.cli.ruleset)
  implementation(libs.ktlint.rule.engine.core)
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
