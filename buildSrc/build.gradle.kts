plugins { `kotlin-dsl` }
repositories { mavenCentral(); maven("https://maven.fabricmc.net/") }
dependencies {
    implementation(localGroovy())
    implementation("net.fabricmc:fabric-loader:0.19.5")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(25) }
