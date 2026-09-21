plugins {
    alias(libs.plugins.kotlin.serialization)
    id("com.uesar.postapp.android.compose")
    id("com.uesar.postapp.hilt")
}

android {
    namespace = "com.uesar.postapp.post.presentation"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(project(":post:domain"))

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
}
