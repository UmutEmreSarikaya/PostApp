plugins {
    id("com.uesar.postapp.android.library")
    id("com.uesar.postapp.hilt")
}

android {
    namespace = "com.uesar.postapp.post.data"
}

dependencies {
    implementation(project(":post:domain"))
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
}
