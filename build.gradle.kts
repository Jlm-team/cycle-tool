plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    id("org.jetbrains.intellij") version "1.4.0"
}

group = "team.jlm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    version.set("2021.2")
    type.set("IC") // Target IDE Platform

    plugins.set(
        listOf(
            "com.intellij.java",
            "Git4Idea"
        )
    )
}
dependencies {
    // https://mvnrepository.com/artifact/guru.nidi/graphviz-java
    //graphviz java的绘图库
    implementation("guru.nidi:graphviz-java:0.18.1") {
        //这个依赖已经在idea的jbr里面存在了
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r"){
        //这个依赖已经在idea的jbr里面存在了
        exclude("org.slf4j", "slf4j-api")
    }
//    implementation("com.github.adedayo.intellij.sdk:git4idea:142.1")

}

sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin")
    getByName("test").java.srcDirs("src/test/kotlin")
}

tasks {
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
