## 2024-07-29 - Kotlin Collection Processing Eagerness
**Learning:** In Kotlin, collection operations like `distinctBy` followed by `take` evaluate eagerly. When applied to large collections (like thousands of local tracks), `distinctBy` will process the entire list and create intermediate collections before `take` is applied.
**Action:** Use `.asSequence()` before chaining operations like `distinctBy` and `take`, and terminate with `.toList()`. This evaluates operations lazily, meaning `take(5)` will stop processing after finding 5 distinct items, completely avoiding processing the rest of the list.
## 2024-05-19 - Replacing pre-sized ArrayList + for loop with List(size) constructor
**Learning:** In Kotlin, creating a `List` using the functional constructor `List(size) { index -> ... }` can be slightly faster and is definitely cleaner than manually sizing an `ArrayList` and using a `for` loop to `.add()` items, even when the `ArrayList` is pre-sized.
**Action:** Default to the `List(size) { ... }` constructor when mapping indexed access (like from an Android framework class or external API that doesn't provide an Iterator) into a Kotlin List.
