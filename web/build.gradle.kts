plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    nodeProjectDir.set(file(projectDir))
    version.set("20")
    npmInstallCommand.set("ci")
}
