/**
 * NOTE: This is entirely optional and basics can be done in `settings.gradle.kts`
 */

repositories {
    maven {
        url = uri("https://www.cursemaven.com")
    }
}

dependencies {
    implementation("curse.maven:hyui-1431415:7731691")
}