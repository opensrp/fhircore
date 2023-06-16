plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    //id("ktlint-kotlin-common")
}

dependencies {
    implementation("com.pinterest.ktlint:ktlint-cli-ruleset-core:0.49.1")
    implementation("com.pinterest.ktlint:ktlint-rule-engine-core:0.49.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}