plugins {
    `java-library`
    `maven-publish`
}

group = project.group
version = project.version

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "pulsify"
            url = uri("https://repo.bxteam.org/releases/")

            if (version.toString().endsWith("-SNAPSHOT")) {
                url = uri("https://repo.bxteam.org/snapshots/")
            }

            credentials.username = System.getenv("REPO_USERNAME")
            credentials.password = System.getenv("REPO_PASSWORD")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name.lowercase()
            version = project.version.toString()
            from(components.getByName("java"))
        }
    }
}
