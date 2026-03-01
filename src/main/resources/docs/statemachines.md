# State Machines
Every model type has a **state machine** that defines its lifecycle. A state machine has:
- A **void state** — handles events that create new model instances (no model exists yet).
- One or more **instance states** — handle events on existing model instances.

Each state declares which **events** it can handle and what **actions** (executables) to perform: create a model, update it, delete it, transition to another state, or trigger a job.

```kotlin
enum class AuthorStates { Amateur, Professional }

fun authorStateMachine(collections: MyCollections) = stateMachine<Author, AuthorStates, Context, MyCollections> {
    event(CreateAuthor) {
        // validation rules
    }
    voidState {
        onEvent(CreateAuthor) {
            createModel(Amateur, ::newAuthor)
        }
    }

    state(Amateur) {
        onEvent(ImproveAuthor) {
            unmanagedJob(::showNotification, onCondition = ShouldSendNotificationAlgorithm::execute)
            transitionTo(Improving)
        }
    }
    
    // other states
}
```
