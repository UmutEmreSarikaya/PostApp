plugins {
    id("com.uesar.postapp.android.compose")
    id("com.uesar.postapp.hilt")
}

android {
    namespace = "com.uesar.postapp.post.presentation"
}

dependencies {
    implementation(project(":post:domain"))

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.coil.compose)
}
