import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.dokka") version "1.9.0"
}

group = "top.kkoishi.stg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(File(rootDir, "doc"))
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}