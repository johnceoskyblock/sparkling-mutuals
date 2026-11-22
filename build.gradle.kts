import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import buildlogic.Edition

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
    kotlin("jvm") version "2.4.20"
}

val targetMinecraft = property("minecraft_version").toString()
val edition = Edition.parse(providers.gradleProperty("edition").orElse("standard").get())
val editionSource = rootProject.file("src/${edition.sourceDirectory}")
val standardTests = rootProject.file("src/standardTest")
if (edition != Edition.FULL) layout.buildDirectory = layout.projectDirectory.dir("build/${edition.id}")
val developerBuild = providers.gradleProperty("developerBuild").map { it.toBooleanStrict() }.orElse(false)
val modVersion = property("mod_version").toString() + if (developerBuild.get()) "-dev" else ""
version = modVersion
group = "net.johnceo.sparklingmutuals"
base.archivesName = "sparkling-mutuals"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://maven.azureaaron.net/releases") }
        filter { includeModule("net.azureaaron", "hm-api") }
    }
    exclusiveContent {
        forRepository { maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
        filter { includeGroup("me.djtheredstoner") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$targetMinecraft")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation(include("net.azureaaron:hm-api:${property("hm_api_version")}")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth.version")}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    runs {
        named("client") {
            ideConfigGenerated(true)
            runDir("../../run/$targetMinecraft")
            if (providers.gradleProperty("clientSmoke").isPresent) {
                vmArg("-Ddevauth.enabled=false")
                vmArg("-Dmixin.debug.verbose=true")
                vmArg("-Dmixin.debug.export=true")
            }
        }
        named("server") { ideConfigGenerated(false) }
    }
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = true
}
java { withSourcesJar() }
if (edition == Edition.FULL) {
    sourceSets.main { java.srcDir(rootProject.file("src/updater/java")) }
    sourceSets.test { java.srcDir(rootProject.file("src/updaterTest/java")) }
}

// Overlay sources replace corresponding shared files before compilation, including in Standard sources JARs.
fun editionSources(set: String, folders: List<File>) {
    val replacements = folders.flatMap { folder ->
        val kotlinDir = folder.resolve("kotlin")
        kotlinDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.relativeTo(kotlinDir).invariantSeparatorsPath to it }.toList()
    }.toMap()
    val exclusions = folders.flatMap { folder ->
        folder.resolve("exclude.txt").takeIf(File::isFile)?.readLines().orEmpty()
            .filter { it.startsWith("kotlin/") }.map { it.removePrefix("kotlin/") }
    }.toSet() + replacements.keys
    if (exclusions.isEmpty()) return
    val output = layout.buildDirectory.dir("generated/edition/$set")
    val prepare = tasks.register("prepare${set.replaceFirstChar(Char::uppercaseChar)}Edition") {
        inputs.files(replacements.values).withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.property("minecraft", targetMinecraft)
        outputs.dir(output)
        doLast {
            val destination = output.get().asFile
            // Replace only this task's generated tree. A removed override must not remain on the classpath.
            project.delete(destination)
            replacements.forEach { (path, source) ->
                val target = destination.resolve(path)
                target.parentFile.mkdirs()
                if (source.useLines { lines -> lines.any { it.trimStart().startsWith("//?") } }) {
                    stonecutter.process(source, target.absolutePath)
                } else source.copyTo(target, overwrite = true)
            }
        }
    }
    kotlin.sourceSets.named(set) {
        kotlin.exclude { it.path in exclusions && !it.file.toPath().startsWith(output.get().asFile.toPath()) }
    }
    val compile = if (set == "main") "compileKotlin" else "compileTestKotlin"
    tasks.named<KotlinCompile>(compile) { source(files(output).builtBy(prepare)) }
    if (set == "main") tasks.named<Jar>("sourcesJar") {
        eachFile {
            if (path in exclusions && !file.toPath().startsWith(output.get().asFile.toPath())) exclude()
        }
        from(files(output).builtBy(prepare))
    }
}
val withoutUpdates = rootProject.file("src/noUpdates")
if (edition != Edition.FULL) {
    editionSources("main", if (edition == Edition.STANDARD) listOf(withoutUpdates, editionSource) else listOf(withoutUpdates))
    val noUpdateTests = rootProject.file("src/noUpdatesTest")
    editionSources("test", if (edition == Edition.STANDARD) listOf(noUpdateTests, standardTests) else listOf(noUpdateTests))
}
if (edition == Edition.STANDARD) {
    val standardResources = editionSource.resolve("resources")
    sourceSets.main {
        resources.exclude { standardResources.resolve(it.path).isFile }
    }
    tasks.processResources { from(standardResources) }
    tasks.named<Jar>("sourcesJar") {
        eachFile {
            if (standardResources.resolve(path).isFile && !file.toPath().startsWith(standardResources.toPath())) exclude()
        }
        from(standardResources)
    }
}

// Local, test-classpath research only; does not publish data or change the target matrix.
if (targetMinecraft == "26.1.2") tasks.register<JavaExec>("replayShardMarket") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "net.johnceo.sparklingmutuals.skyblock.market.ShardReplayCli"
    doFirst {
        args(providers.gradleProperty("marketManifest").get(), providers.gradleProperty("marketReport").get())
        if (providers.gradleProperty("marketSelectTraining").isPresent) args("select-training")
        else if (providers.gradleProperty("marketExportSamples").isPresent) args("export-samples")
    }
}
tasks.processResources {
    val properties = mapOf("version" to modVersion, "minecraft" to targetMinecraft, "developer" to developerBuild.get(), "edition" to edition.id)
    inputs.properties(properties)
    filesMatching("fabric.mod.json") { expand(properties) }
}
tasks.withType<JavaCompile>().configureEach { options.release = 25 }
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
tasks.jar {
    archiveFileName = edition.jarName(modVersion, targetMinecraft)
    from(rootProject.file("licenses")) { into("licenses") }
    from(rootProject.file("LICENSE")) { rename { "LICENSE_sparkling-mutuals" } }
}
tasks.named<Jar>("sourcesJar") { from(rootProject.file("licenses")) { into("licenses") } }
