plugins {
    id("pulsify-platform")
}

dependencies {
    implementation(project(":pulsify-sdk"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}
