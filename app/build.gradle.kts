import com.android.SdkConstants
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
        freeCompilerArgs.addAll(listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        ))
    }
}

android {
    namespace = libs.versions.project.app.packageName.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = libs.versions.project.app.packageName.get()
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionName = libs.versions.project.app.versionName.get()
        versionCode = libs.versions.project.app.versionCode.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUILD_TIME", "\"" + SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis()) + "\"")
        ndk {
            abiFilters.add(SdkConstants.ABI_ARM64_V8A)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    androidResources {
        additionalParameters += listOf("--stable-ids", "stableIds.txt")
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x60")
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint { checkReleaseBuilds = false }
    val properties = Properties()
    runCatching { properties.load(project.rootProject.file("local.properties").inputStream()) }
    // 签名配置优先读项目根目录的 keystore.properties（已 gitignore），
    // 其次 local.properties，最后环境变量
    val keystoreProperties = Properties()
    runCatching { keystoreProperties.load(project.rootProject.file("keystore.properties").inputStream()) }
    fun signingProp(key: String) =
        keystoreProperties.getProperty(key) ?: properties.getProperty(key) ?: System.getenv(key)
    val ksPath = signingProp("KEYSTORE_PATH")
    val ksPWD = signingProp("KEYSTORE_PWD")
    val kAlias = signingProp("KEY_ALIAS")
    val kPWD = signingProp("KEY_PWD")
    signingConfigs {
        register("release") {
            storeFile = rootProject.file(ksPath)
            storePassword = ksPWD
            keyAlias =kAlias
            keyPassword = kPWD
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
        applicationVariants.all {
            outputs.all {
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                    "${libs.versions.project.name.get()}_${versionName}_${versionCode}_${buildType.name}_${System.currentTimeMillis() / 1000}.apk"
            }
        }
    }
}

dependencies {
    implementation("androidx.preference:preference:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Hook 相关API
    implementation(libs.androidx.remote.creation.core)
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.ui)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dexkit)
    implementation(project(mapOf("path" to ":hyperx-compose")))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}