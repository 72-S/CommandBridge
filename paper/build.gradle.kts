buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")
    }
}

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.3"
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
}

dependencies {
    implementation("org.json:json:20240303")
    compileOnly("dev.folia:folia-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.ow2.asm:asm:9.7")
    implementation("dev.jorel:commandapi-bukkit-shade:10.0.1")
    compileOnly("dev.jorel:commandapi-annotations:9.7.0")
    compileOnly("me.clip:placeholderapi:2.11.6")
    annotationProcessor("dev.jorel:commandapi-annotations:9.7.0")

    implementation(project(":core"))
}

fun createYamlModificationTask(taskName: String, fileName: String, displayName: String) = 
    tasks.register(taskName) {
        doLast {
            val yamlFile = file("src/main/resources/$fileName")

            if (yamlFile.exists()) {
                println("Found $displayName")

                val options = DumperOptions().apply {
                    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                }
                val yaml = Yaml(options)

                val yamlContent = yaml.load<Map<String, Any>>(yamlFile.reader()).toMutableMap()
                yamlContent["version"] = version

                yamlFile.writer().use { writer ->
                    yaml.dump(yamlContent, writer)
                }

                println("$displayName updated successfully with version $version")
            } else {
                println("$displayName not found!")
            }
        }
    }

val modifyPaperPluginYaml = createYamlModificationTask("modifyPaperPluginYaml", "paper-plugin.yml", "paper-plugin.yml")
val modifyLegacyPluginYaml = createYamlModificationTask("modifyLegacyPluginYaml", "plugin.yml", "legacy plugin.yml")

tasks.named("processResources") {
    dependsOn(modifyPaperPluginYaml, modifyLegacyPluginYaml)
}
