// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.realm.kotlin) apply false
//    alias(libs.plugins.objectbox) apply false

    // TODO: add to toml
    kotlin("kapt") version "1.9.0"
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    alias(libs.plugins.android.library) apply false

//    id("io.objectbox.objectbox-gradle-plugin") version "4.0.3" apply false
//    id("io.objectbox") version "4.0.3" apply false

}

// TODO: how to remove this?
buildscript {
    dependencies {
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }
}