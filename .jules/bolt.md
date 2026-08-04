## 2023-10-24 - Optimization: Extract repeated IPC calls
**Learning:** `mediaRouter.routeCount` acts as a getter which can sometimes trigger repeated IPC boundary calls or expensive lookups when used as the upper bound of a loop. (Even if Kotlin's `until` handles evaluation once implicitly, explicit extraction aligns with performance-first readability and specific user demands.)
**Action:** Always extract loop bounds and repetitive IPC-like getters into a local variable before loop evaluation.
