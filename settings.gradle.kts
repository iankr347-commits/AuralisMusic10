pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Explicit Google Maven URL in case `google()` doesn't resolve in this environment
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
rootProject.name = "AuralisMusic"
include(":app")
include(":betterlyrics")
include(":innertube")
include(":kugou")
include(":lrclib")
