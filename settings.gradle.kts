rootProject.name = "Pulsify"

setOf(
    "sdk"
).forEach {
    subProject(it)
}

fun subProject(name: String) {
    include(":pulsify-$name")
    project(":pulsify-$name").projectDir = file(name)
}

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
