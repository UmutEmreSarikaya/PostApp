plugins {
    id("com.uesar.postapp.android.library")
}

android {
    namespace = "com.uesar.postapp.post.domain"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
