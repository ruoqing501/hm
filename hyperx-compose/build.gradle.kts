import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    // 添加 Kotlin Android 插件
    kotlin("android")
    // 使用完整的插件ID而不是简写
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
}

// 将 kotlin 配置块移动到 android 之后
android {
    namespace = "dev.lackluster.hyperx.compose"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // 如果需要指定 Compose Compiler 版本，可以在这里配置
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

@Suppress("UseTomlInstead")
dependencies {
    // Kotlin 标准库依赖
    implementation(kotlin("stdlib"))


    api("top.yukonga.miuix.kmp:miuix:0.5.2")
    api("dev.chrisbanes.haze:haze:1.7.1")
    // 液态玻璃引擎（酷安 16.6 底部菜单栏同款）
    // 注：2.0.0+ 正式版基于 Compose 1.12 构建，要求 compileSdk 37 / AGP 9.1；
    // 2.0.0-alpha03 基于 Compose 1.10.1，与当前 AGP 8.12.3 / compileSdk 36 兼容且 API 完整
    api("io.github.kyant0:backdrop:2.0.0-alpha03")
    // drawBackdrop 的 Capsule 形状来自 shapes 库（backdrop 运行时也会传递依赖，此处显式声明以供编译期使用）
    api("io.github.kyant0:shapes:1.2.0")
    api("androidx.compose.foundation:foundation:1.10.2")
    api("androidx.activity:activity-compose:1.12.3")
    api("androidx.navigation:navigation-compose:2.9.7")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    api("com.github.promeg:tinypinyin:2.0.3")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
}