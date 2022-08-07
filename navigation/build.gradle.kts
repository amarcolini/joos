plugins {
    kotlin("jvm")
    java
    `maven-publish`
    id("org.jetbrains.dokka")
}

extra {
    extra["dokkaName"] = "Navigation"
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "9"
        apiVersion = "1.5"
    }
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")

    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

    testImplementation("org.knowm.xchart:xchart:3.8.1")
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "navigation"
            artifact(sourcesJar.get())
            pom {
                name.set("Joos")
                description.set("A comprehensive kotlin library designed for FTC.")
                url.set("https://github.com/amarcolini/joos")
                developers {
                    developer {
                        id.set("amarcolini")
                        name.set("Alessandro Marcolini")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "testRepo"
            url = uri("../testRepo")
        }
    }
}

tasks.clean.configure {
    doFirst {
        delete(file("graphs"), file("config"))
    }
}