import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApiWarning()

    jvmToolchain(21)

    android {
        namespace = "com.hrm.codehigh.parser"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {}

        // 将 consumer-rules.pro 打包到 AAR，下游开启 R8 时自动生效
        // 仅声明类元数据属性依赖，不锁定任何具体类/成员，对下游 R8 优化零影响
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    coordinates("io.github.zusrsoft", "codehighlight-parser", libs.versions.codehigh.get())

    pom {
        name.set("CodeHigh Parser")
        description.set(
            """
            Cross-platform code tokenization and incremental parsing layer for CodeHigh.
            """.trimIndent()
        )
    }
}
