plugins {
    id("pulsify-java")
    id("pulsify-publish")
    id("pulsify-repositories")
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    compileOnly("org.apache.logging.log4j:log4j-core:2.23.0")
}
