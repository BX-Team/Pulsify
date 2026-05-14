plugins {
    id("pulsify-java")
    id("pulsify-repositories")
    id("com.gradleup.shadow")
}

val platformDisplayName = when (project.name) {
    "paper"    -> "Paper"
    "bungee"   -> "BungeeCord"
    "velocity" -> "Velocity"
    else       -> project.name.replaceFirstChar { it.uppercase() }
}

tasks.shadowJar {
    archiveBaseName.set("Pulsify-$platformDisplayName")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    relocate("com.fasterxml.jackson", "org.bxteam.pulsify.shaded.jackson")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
