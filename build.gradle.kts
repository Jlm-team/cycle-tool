plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
    id("org.jetbrains.intellij") version "1.6.0"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.20"
}

group = "team.jlm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    jcenter()
    maven{
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
    implementation("guru.nidi:graphviz-java:0.18.1") {
        //这个依赖已经在idea的jbr里面存在了
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.8.9")

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
        sinceBuild.set("212.*")
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
