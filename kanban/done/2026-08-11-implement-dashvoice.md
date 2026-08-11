# Task Tree

- [done] Implement the DashVoice application
  - [done] Determine the Android recognition-service contract
  - [done] Determine the DashScope realtime protocol
  - [done] Define the local credential and settings model
  - [done] Define the implementation task tree
  - [done] Define build and device validation
  - [done] Implement local configuration
    - [done] Add Base URL, language, and VAD models
    - [done] Add the Room database and DAO
    - [done] Persist API Key, Base URL, and settings in Room
  - [done] Implement MVVM application architecture
    - [done] Add the repository abstraction and application container
    - [done] Add settings state and ViewModel
    - [done] Add recognition state, events, and ViewModel
    - [done] Keep activities limited to platform integration
    - [done] Keep Compose screens stateless
    - [done] Convert recognition adapters to coroutine Flow
    - [done] Remove callback interfaces from ViewModels
  - [done] Implement DashScope realtime recognition
    - [done] Build and parse protocol events
    - [done] Capture and stream PCM audio
    - [done] Handle VAD, completion, and errors
  - [done] Expose Android recognition integrations
    - [done] Register the recognition service
    - [done] Add the recognition action activity
    - [done] Return partial and final results
  - [done] Implement the Compose user interface
    - [done] Configure credentials and endpoint
    - [done] Request microphone permission
    - [done] Add recognition testing and setup guidance
  - [done] Validate the installable application
    - [done] Run unit tests, lint, and build
    - [done] Run IDEA analysis and build
    - [done] Install and inspect on the phone

# Details

Build a pure Compose Android application with package `io.github.stream29.dashvoice`. Expose DashScope speech recognition through Android's standard speech-recognition service so an existing input method can invoke it without embedding a second keyboard.

The first installable version stores a user-supplied DashScope API Key and Base URL as plaintext fields in Room, as explicitly requested by the user.

Implement both integration surfaces used by Android input methods:

- An exported `RecognitionService` for callers using `SpeechRecognizer`.
- An exported `ACTION_RECOGNIZE_SPEECH` activity for callers that launch the standard recognition UI. This is required for compatibility with the currently installed input-method setup, which does not hold `RECORD_AUDIO`.

Use `qwen3-asr-flash-realtime` through the official realtime WebSocket protocol. Stream 16 kHz mono PCM16 audio, use server VAD, return partial and final Android recognition results, and omit the DashScope language field by default to preserve automatic language detection.

Persist one Room configuration row containing the plaintext API Key, Base URL, language, and VAD preset. The user supplies the Base URL directly; append the fixed `qwen3-asr-flash-realtime` model query parameter when opening the WebSocket.
