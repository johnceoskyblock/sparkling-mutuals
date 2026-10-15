import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
    kotlin("jvm") version "2.4.20"
}

val targetMinecraft = property("minecraft_version").toString()
val modVersion = property("mod_version").toString()
version = modVersion
group = "net.johnceo.sparklingmutuals"
base.archivesName = "sparkling-mutuals"

repositories {
    mavenCentral()
    maven("https://maven.notenoughupdates.org/releases")
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
    implementation(include("org.notenoughupdates.moulconfig:${property("moulconfig_artifact")}:4.8.0")!!)
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
tasks.processResources {
    val properties = mapOf("version" to modVersion, "minecraft" to targetMinecraft)
    inputs.properties(properties)
    filesMatching("fabric.mod.json") { expand(properties) }
}
tasks.withType<JavaCompile>().configureEach { options.release = 25 }
kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }
java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}
tasks.jar {
    archiveFileName = "sparkling-mutuals-$modVersion+mc$targetMinecraft.jar"
    from(rootProject.file("licenses")) { into("licenses") }
    from(rootProject.file("LICENSE")) { rename { "LICENSE_sparkling-mutuals" } }
}
tasks.named<Jar>("sourcesJar") { from(rootProject.file("licenses")) { into("licenses") } }
