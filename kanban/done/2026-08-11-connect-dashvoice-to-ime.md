# Task Tree

- Connect DashVoice to the installed input method
  - [done] Inspect the current Android voice-input defaults
  - [done] Identify the Fcitx voice invocation route
  - [done] Set DashVoice as the recognition service
  - [done] Verify the Kõnele wrapper route
  - [done] Expose DashVoice as a thin voice input method
  - [done] Enable the DashVoice voice input method
  - [done] Select DashVoice in Fcitx
  - [done] Verify automatic return after transcription

# Details

Connect DashVoice to the current Fcitx5 Rime input method without replacing Fcitx as the default keyboard. Preserve the existing Fcitx default and configure only its microphone action.

The installed Fcitx5 version switches to an enabled input method exposing a `voice` subtype. Kõnele can wrap the DashVoice `RecognitionService`, but Kõnele 1.9.17 does not automatically switch back after committing a final result.

Add a one-shot DashVoice voice IME that delegates recognition to the existing coroutine-backed `RecognitionService`, commits partial and final text, and calls `switchToPreviousInputMethod()` after completion. Start audio capture before the DashScope WebSocket handshake and retain about two seconds of pre-roll audio to hide connection latency.

DashVoice is selected as the system recognition service and Fcitx voice input target. Kõnele is no longer required and has been uninstalled.
