plugins {
    application
    `maven-publish`
    kotlin("jvm") version "2.0.0"
}

group = "net.essentuan"
version = project.properties["version"]!! as String

val log4j_version: String by project
val slf4j_version: String by project
val rx_streams_version: String by project
val coroutines_version: String by project
val guava_version: String by project
val gson_version: String by project
val reflections_version: String by project
val dateparser_version: String by project

application {
    mainClass = "net.essentuan.esl.MainKt"
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))

    implementation("org.apache.logging.log4j:log4j-api:$log4j_version")
    implementation("org.slf4j:slf4j-simple:$slf4j_version")

    implementation("org.reactivestreams:reactive-streams:$rx_streams_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("com.google.guava:guava:$guava_version-jre")
    implementation("com.google.code.gson:gson:$gson_version")
    implementation("org.reflections:reflections:$reflections_version")
    implementation("com.github.sisyphsu:dateparser:$dateparser_version")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "esl"

            from(components["java"])
        }
    }
}