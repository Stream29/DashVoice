# Task Tree

- Refactor DashScope realtime transport
  - [done] Identify the callback-based boundary
  - [done] Add coroutine-native Ktor WebSockets
  - [done] Model protocol messages with kotlinx.serialization
  - [done] Consume server frames as Flow
  - [done] Remove OkHttp WebSocketListener
  - [done] Update protocol and endpoint tests
  - [done] Validate a real DashScope session

# Details

Replace the app-owned `WebSocketListener` implementation with Ktor's suspending WebSocket session and channel APIs. Use `kotlinx.serialization` data models for the DashScope JSON protocol and transform incoming frames through Flow.

Use the CIO engine so application code no longer owns an OkHttp callback adapter. Keep the existing service-facing event Flow and cancellation semantics while replacing protocol strings with serializable event types.

Pin Ktor to the Kotlin 2.2-compatible 3.3 line used by this project, and retain the existing 16 kHz PCM capture, VAD, timeout, and terminal-event behavior.
