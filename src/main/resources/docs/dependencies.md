# Dependencies

To use Klerk in your project, you need to add the following dependencies to your build.gradle.kts file:

```kotlin
implementation(platform("dev.klerkframework:klerk-bom:1.0.0-beta.5"))
implementation("dev.klerkframework:klerk")
```

Note that klerk-bom includes ktor-bom.

Klerk-web is an optional dependency. It can automatically generate an admin UI, and it makes creating a server-side rendering 
application easier. To use it, you also need to add the following dependency:
```kotlin
implementation("dev.klerkframework:klerk-web")
```

If you want to generate a GraphQL API, you also need to add the following dependency:
```kotlin
implementation("dev.klerkframework:klerk-graphql")
```

You currently also need to 'maven("https://jitpack.io")' to settings.gradle.kts:
```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral() {
            content { excludeGroup("dev.klerkframework") }
        }
        maven("https://jitpack.io") {
            content { includeGroup("dev.klerkframework") }
        }    
    }
}
```
