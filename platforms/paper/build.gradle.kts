plugins {
    id("pulsify-platform")
}

dependencies {
    implementation(project(":pulsify-sdk"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}
