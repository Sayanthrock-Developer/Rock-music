## 2025-01-01 - [OkHttp coroutines support]
**Learning:** Introduce a `Call.await()` extension using `suspendCancellableCoroutine` for wrapping synchronous OkHttp API blocking calls (`client.newCall(request).execute()`) into suspend functions instead.
**Action:** When making asynchronous requests within a suspend function block with OkHttp, always prefer using `.await()` instead of `.execute()` to prevent thread starvation and allow coroutines to safely yield thread execution while doing I/O.
