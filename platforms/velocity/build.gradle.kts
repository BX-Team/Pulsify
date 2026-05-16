plugins {
    id("pulsify-platform")
    id("net.kyori.blossom") version "2.2.0"
}

dependencies {
    implementation(project(":pulsify-sdk"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

sourceSets {
    main {
        blossom.javaSources {
            property("version", project.version.toString())
        }
    }
}
