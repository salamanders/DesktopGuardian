import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.driver.sqlite)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.jna)
            implementation(libs.jna.platform)
            implementation(libs.logback.classic)
        }
    }
}

sqldelight {
    databases {
        create("desktopguardian") {
            packageName.set("info.benjaminhill.desktopguardian.db")
        }
    }
}

compose.desktop {
    application {
        mainClass = "info.benjaminhill.desktopguardian.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "desktop-guardian"
            packageVersion = "1.0.0"
            description = "A simple system monitor for installed apps and extensions."
            copyright = "© 2024 Benjamin Hill"
            vendor = "Benjamin Hill"

            linux {
                menuGroup = "Utility"
                shortcut = true
            }

            macOS {
                bundleID = "info.benjaminhill.desktopguardian"
                dockName = "Desktop Guardian"
                // TODO: Uncomment and ensure .icns/.ico files exist to enable native icons.
                // Conversion from SVG to .icns/.ico is required as jpackage does not support SVG directly.
                // iconFile.set(project.file("src/commonMain/composeResources/drawable/desktop_guardian_icon.icns"))
            }

            windows {
                menuGroup = "Desktop Guardian"
                shortcut = true
                upgradeUuid = "12345678-1234-1234-1234-123456789012" // Fixed UUID for upgrades
                // iconFile.set(project.file("src/commonMain/composeResources/drawable/desktop_guardian_icon.ico"))
            }
        }
    }
}
