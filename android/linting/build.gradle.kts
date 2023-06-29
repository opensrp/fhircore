plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
}

dependencies {
  implementation(libs.ktlint.cli.ruleset)
  implementation(libs.ktlint.rule.engine.core)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}
