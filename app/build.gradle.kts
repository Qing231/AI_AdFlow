import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun configValue(name: String, defaultValue: String = ""): String {
    return localProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
        ?: defaultValue
}

fun configValue(vararg names: String, defaultValue: String = ""): String {
    return names.firstNotNullOfOrNull { name ->
        localProperties.getProperty(name)
            ?: providers.environmentVariable(name).orNull
    } ?: defaultValue
}

fun buildConfigString(value: String): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.example.aiadflow"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.aiadflow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OPENAI_API_KEY", buildConfigString(configValue("OPENAI_API_KEY")))
        buildConfigField(
            "String",
            "AI_SUMMARY_API_KEY",
            buildConfigString(configValue("AI_SUMMARY_API_KEY", "DASHSCOPE_API_KEY", "OPENAI_API_KEY"))
        )
        buildConfigField(
            "String",
            "AI_SUMMARY_ENDPOINT",
            buildConfigString(
                configValue(
                    "AI_SUMMARY_ENDPOINT",
                    defaultValue = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
                )
            )
        )
        buildConfigField("String", "AI_SUMMARY_MODEL", buildConfigString(configValue("AI_SUMMARY_MODEL", "qwen-plus")))
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "OldTargetApi"
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
