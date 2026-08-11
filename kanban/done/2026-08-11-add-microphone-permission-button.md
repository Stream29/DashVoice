# Task Tree

- [done] Add an explicit microphone permission button
  - [done] Inspect the existing permission flow
  - [done] Add a standalone permission request
  - [done] Add a visible grant button
  - [done] Preserve test-triggered permission behavior
  - [done] Replace the placeholder launcher icon
    - [done] Add adaptive and legacy microphone assets
    - [done] Add a monochrome themed icon
    - [done] Register launcher and round icons
  - [done] Validate the build and phone flow

# Details

The current settings screen only requests `RECORD_AUDIO` indirectly after the user starts a recognition test. Add an immediately visible button to the status section that opens Android's runtime permission dialog without starting recognition.

Keep the existing test-triggered request, but distinguish it from the standalone request in the ViewModel so granting permission alone never starts recording.

Validate with unit tests, Android lint, an IDEA rebuild, installation, and the Android permission dialog on the connected phone.

The user selected a microphone-based launcher icon. Replace the square scaffold vector with a modern adaptive icon while keeping a legacy vector fallback.
