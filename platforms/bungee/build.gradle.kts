plugins {
    id("pulsify-platform")
    id("de.eldoria.plugin-yml.bungee") version "0.9.0"
}

dependencies {
    implementation(project(":sdk"))
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
}

bungee {
    name = "Pulsify"
    description = "Pulsify analytics SDK for Bungeecord servers"
    author = "BX Team"

    main = "org.bxteam.pulsify.bungee.PulsifyPlugin"
}
