# Models

A **model** is a data class that represents a domain entity (e.g. `Author`, `Book`). Models are managed by Klerk and always have an associated state.
The Klerk Model class contains metadata and properties:

```kotlin
public data class Model<T : Any>(
    val id: ModelID<T>,
    val createdAt: Instant,
    val lastPropsUpdateAt: Instant,
    val lastStateTransitionAt: Instant,
    val state: String,
    val timeTrigger: Instant?,
    val props: T,
)
```

The metadata is managed by Klerk and cannot be manipulated by the developer directly.
The developer defines her own models by creating data classes that contain properties. The properties must be wrapped in user defined classes that extends **DataContainer** subclasses (e.g. `StringContainer`, `IntContainer`, `BooleanContainer`), never plain Kotlin types. This enables built-in validation, authorization, and UI labelling.

```kotlin
data class Author(
    val firstName: FirstName,
    val lastName: LastName,
    val phoneNumber: PhoneNumber
)

class FirstName(value: String) : StringContainer(value) {
    override val minLength = 1
    override val maxLength = 30
    override val maxLines = 1
}

class LastName(value: String) : StringContainer(value) {
    override val minLength = 1
    override val maxLength = 50
    override val maxLines = 1
}

class PhoneNumber(value: String) : StringContainer(value) {
    override val minLength = 7
    override val maxLength = 15
    override val maxLines = 1
    override val regexPattern = "/^\\+?[1-9][0-9]{7,14}\$/"
}
```

The properties can also contain references to other models. E.g.:
```kotlin
data class Book(
    val author: ModelID<Author>
)
```

The data classes can have custom validation by overriding validators, e.g.:
```kotlin
override fun validators(): Set<() -> PropertyCollectionValidity> = setOf(::noAuthorCanBeNamedJamesClavell)
```

It is also possible to tag the data container. This can be used in authorization and UI labeling. E.g.:
```kotlin
override val tags: Set<String> = setOf("PII")
```

When reading a model, the developer receives a Model<T> object and can read the metadata and properties.
