import java.util.Properties

rootProject.name = "Tombale"

// 1. Load properties from gradle.properties
val props = Properties()
file("gradle.properties").inputStream().use { props.load(it) }
// 2. Access your property
val modVersion = props.getProperty("modVersion")
plugins {
    // See documentation on https://scaffoldit.dev
    id("dev.scaffoldit") version "0.2.+"
}

// Would you like to do a split project?
// Create a folder named "common", then configure details with `common { }`

hytale {

    usePatchline("pre-release")
    useVersion("latest")

    repositories {
        // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
    }

    dependencies {
        // Any external dependency you also want to include
    }
    manifest {
        Group = "Diamssword"
        Name = "Tombale"
        Main = "com.diamssword.tombale.Tombale"
        Version = modVersion
        Description = "A grave mod for Hytale!"
        IncludesAssetPack = true
    }
}