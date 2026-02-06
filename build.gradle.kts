import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.6.11"
}

group = "io.github.mdalfre"
version = "1.0.6"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("org.bytedeco:opencv:4.9.0-1.5.10")
    runtimeOnly("org.bytedeco:opencv:4.9.0-1.5.10:windows-x86_64")
    runtimeOnly("org.bytedeco:openblas:0.3.26-1.5.10:windows-x86_64")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "io.github.mdalfre.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "MUreset"
            packageVersion = "1.0.0"
            windows {
                console = false
                menuGroup = "MUreset"
                modules(
                    "jdk.unsupported"
                )
               iconFile.set(project.file("src/main/resources/mu-icon.ico"))
            }
        }
    }
}
