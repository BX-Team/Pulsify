plugins {
    id("pulsify-platform")
}

dependencies {
    implementation(project(":pulsify-sdk"))
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
}
