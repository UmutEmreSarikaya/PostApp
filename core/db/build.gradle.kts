plugins {
    id("com.uesar.postapp.android.library")
    id("com.uesar.postapp.hilt")
}

android {
    namespace = "com.uesar.postapp.core.db"
}

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}
