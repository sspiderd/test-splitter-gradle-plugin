import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.21"
    `java-gradle-plugin`
    maven
    id("com.gradle.plugin-publish") version "0.10.1"
}

repositories {
    jcenter()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
}

gradlePlugin {
    plugins {
        create("testSplitterPlugin") {
            id = "org.sspiderd.testsplitter"
            displayName = "Test Splitter Plugin"
            description = "This plugin splits tests for gradle into individual file to allow concurrent testing"
            implementationClass = "org.sspiderd.testsplitter.TestSplitterPlugin"
        }
    }
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

pluginBundle {
    website = "https://github.com/sspiderd/test-splitter-gradle-plugin.git"
    vcsUrl = "https://github.com/sspiderd/test-splitter-gradle-plugin.git"
    tags = listOf("test", "tests", "split")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}