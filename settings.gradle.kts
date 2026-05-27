rootProject.name = "Pulsify"

include(":sdk")

setOf(
    "paper",
    "velocity",
    "bungee"
).forEach {
    platformProject(it)
}

fun platformProject(name: String) {
    include(":platforms:$name")
    project(":platforms:$name").projectDir = file("platforms/$name")
}
