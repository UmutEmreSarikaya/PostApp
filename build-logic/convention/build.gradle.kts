plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "com.uesar.postapp.kotlin.library"
            implementationClass = "com.uesar.postapp.convention.KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "com.uesar.postapp.android.library"
            implementationClass = "com.uesar.postapp.convention.AndroidLibraryConventionPlugin"
        }
        register("hilt") {
            id = "com.uesar.postapp.hilt"
            implementationClass = "com.uesar.postapp.convention.HiltConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}
