import buildlogic.TargetSpec
import buildlogic.verifyDistribution
import buildlogic.verifyJar
import buildlogic.PublishReleaseTask
import buildlogic.collectDistribution

plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.21" apply false
    kotlin("jvm") version "2.4.20" apply false
}
stonecutter active "26.1.2"

val releaseVersion = providers.gradleProperty("mod_version")
tasks.register("validateReleaseTag") {
    doLast { require(System.getenv("GITHUB_REF") == "refs/tags/v${releaseVersion.get()}") { "Tag must match mod_version" } }
}
val testBuildLogic = tasks.register<GradleBuild>("testBuildLogic") {
    group = "verification"
    description = "Runs the distribution and release build-helper regression tests."
    dir = file("buildSrc")
    tasks = listOf("test")
    buildName = "sparkling-build-logic-tests"
}
val buildAllTargets = tasks.register("buildAllTargets") {
    group = "build"
    description = "Tests/builds every Minecraft target and collects exactly three verified installable JARs."
    dependsOn(testBuildLogic)
}
val verifyDistributions = tasks.register("verifyDistributions") {
    group = "verification"
    description = "Verifies a collected three-JAR distribution without rebuilding it."
    mustRunAfter(buildAllTargets)
}

gradle.projectsEvaluated {
    val targets = subprojects.sortedBy { it.name }
    fun spec(p: Project) = TargetSpec(p.property("minecraft_version").toString(),
        p.property("fabric_api_version").toString(), p.property("hm_api_version").toString(),
        p.property("moulconfig_artifact").toString())
    targets.forEach { p ->
        p.tasks.register("verifyTargetJar") {
            group = "verification"
            dependsOn(p.tasks.named("build"))
            doLast { verifyJar(p.tasks.named<Jar>("jar").get().archiveFile.get().asFile.toPath(), spec(p), releaseVersion.get()) }
        }
    }
    buildAllTargets.configure {
        dependsOn(targets.map { it.tasks.named("verifyTargetJar") })
        doLast {
            val destination = layout.buildDirectory.dir("distributions/${releaseVersion.get()}").get().asFile
            collectDistribution(targets.map { it.tasks.named<Jar>("jar").get().archiveFile.get().asFile.toPath() },
                targets.map(::spec), releaseVersion.get(), destination.toPath())
            logger.lifecycle("Verified three-target distribution: $destination")
        }
    }
    verifyDistributions.configure {
        doLast {
            val directory = providers.gradleProperty("distributionDir").orNull?.let(::file)
                ?: layout.buildDirectory.dir("distributions/${releaseVersion.get()}").get().asFile
            require(directory.isDirectory) { "Missing distribution directory: $directory" }
            verifyDistribution(directory.listFiles()!!.filter { it.isFile && it.extension == "jar" }.map { it.toPath() },
                targets.map(::spec), releaseVersion.get())
            logger.lifecycle("Verified all three installable JARs in $directory")
        }
    }
    tasks.register<PublishReleaseTask>("publishVerifiedRelease") {
        group = "publishing"
        description = "Publishes a complete verified distribution, exclusively in the matching tag-push workflow."
        modVersion = releaseVersion
        targetProperties = targets.associate { p -> val s = spec(p); s.minecraft to "${s.fabricApi}|${s.hmApi}|${s.moulConfig}" }
        distributionDirectory = providers.gradleProperty("distributionDir").map { layout.projectDirectory.dir(it) }
            .orElse(layout.buildDirectory.dir("distributions/${releaseVersion.get()}"))
    }
}
