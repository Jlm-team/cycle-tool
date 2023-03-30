import org.gradle.internal.impldep.org.eclipse.jgit.lib.ObjectChecker.type
import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
    id("org.jetbrains.intellij") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.20"
}
val resourcesDir = "${project.projectDir}/src/main/resources"

group = "team.jlm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    jcenter()
    maven {
        setUrl("https://www.jetbrains.com/intellij-repository/releases")
        setUrl("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
        setUrl("https://mvnrepository.com/")
    }
}
noArg {
    annotation("team.jlm.annotation.NoArg")
    invokeInitializers = true
}
intellij {
    version.set("2021.2")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "com.intellij.java",
            "Git4Idea",
        )
    )
}
dependencies {
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.slf4j:slf4j-api:2.0.6")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("guru.nidi:graphviz-java:0.18.1") {
        //这个依赖已经在idea的jbr里面存在了
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")

}

sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin")
    getByName("test").java.srcDirs("src/test/kotlin")
}

tasks {
    withType<RunIdeTask> {
        println("RunIdeTask")
        jvmArgs("-Xmx2g")
    }
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("212")
        untilBuild.set("222.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
