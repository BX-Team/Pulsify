import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("pulsify-platform")
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
}

dependencies {
    implementation(project(":pulsify-sdk"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
}

bukkit {
    name = "Pulsify"
    description = "Pulsify analytics SDK for Paper servers"
    website = "https://bxteam.org"
    author = "BX Team"

    main = "org.bxteam.pulsify.paper.PulsifyPlugin"
    apiVersion = "1.16"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    foliaSupported = true
}

tasks.compileJava {
    options.compilerArgs.add("-Xlint:deprecation")
}
