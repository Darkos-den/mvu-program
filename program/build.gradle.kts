import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    `maven-publish`
    id("com.jfrog.artifactory")
}

val organization = "darkos"
val repository = "mvu"

val artifactName = "program"
val artifactGroup = "com.$organization.$repository"
val artifactVersion = "1.0.0-rc2"

val repoName = "mvu-program"

val localPropsFile: File = project.rootProject.file("local.properties")
val localProperties = loadProperties(localPropsFile.absolutePath)

val mUsername: String? by localProperties
val mPassword: String? by localProperties

group = artifactGroup
version = artifactVersion

repositories {
    gradlePluginPortal()
    google()
    maven(url = "https://dl.google.com/dl/android/maven2")
    jcenter()
    mavenCentral()

    maven(url = "https://darkos.jfrog.io/artifactory/mvu/") {
        credentials {
            username = mUsername
            password = mPassword
        }
    }
}

android {
    val sdkMin = 26
    val sdkCompile = 30

    compileSdkVersion(sdkCompile)
    defaultConfig {
        minSdkVersion(sdkMin)
        targetSdkVersion(sdkCompile)
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
}

kotlin {
    android("android") {
        publishAllLibraryVariants()
    }
    ios {
        binaries {
            framework {
                baseName = artifactName
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
                implementation("com.darkos.mvu:core:1.0.0-rc1")
            }
        }
        val androidMain by getting
        val iosMain by getting
        val iosArm64Main by getting
        val iosX64Main by getting
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

artifactory {
    val baseUrl = "https://darkos.jfrog.io/artifactory"

    setContextUrl(baseUrl)

    publish(closureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
        setContextUrl(baseUrl)

        repository(closureOf<org.jfrog.gradle.plugin.artifactory.dsl.DoubleDelegateWrapper> {
            invokeMethod("setRepoKey", repoName)
            invokeMethod("setUsername", mUsername)
            invokeMethod("setPassword", mPassword)
        })
    })

    resolve(closureOf<org.jfrog.gradle.plugin.artifactory.dsl.ResolverConfig> {
        repository(closureOf<org.jfrog.gradle.plugin.artifactory.dsl.DoubleDelegateWrapper> {
            invokeMethod("setRepoKey", "mvu")
            invokeMethod("setUsername", mUsername)
            invokeMethod("setPassword", mPassword)
            invokeMethod("setMaven", true)
        })
    })
}

publishing {
    val vcs = "https://darkos.jfrog.io/artifactory/$repoName/"

    publications.filterIsInstance<MavenPublication>().forEach { publication ->
        println(publication.name)

        publication.pom {
            name.set(artifactName)
            description.set(project.description)
            url.set(vcs)

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            scm {
                url.set(vcs)
                tag.set(project.version.toString())
            }
        }
    }

    repositories {
        maven {
            name = "artifactory"
            url =
                uri("https://darkos.jfrog.io/artifactory/$repoName/")
            credentials {
                username = mUsername
                password = mPassword
            }
        }
    }
}