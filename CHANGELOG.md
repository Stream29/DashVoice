# Changelog

## 0.1.1 — 2026-10-24

- Add configurable post-recognition text polishing while preserving the user's
  wording and falling back to the ASR transcript when polishing fails.
- Include required polishing request fields even when their values equal DTO
  defaults, fixing requests rejected for a missing model parameter.
- When CJK-space removal is enabled, remove whitespace between Han characters,
  including repeated and Unicode spaces, while preserving English and Korean
  word spaces and the formatting-off behavior.
- Add regression tests for the polishing request contract and text normalization.
- Increase Android `versionCode` to `2`; retain the existing signing identity.

The original intermittent input-engine failure after returning from voice input
has not been reproduced or resolved. This release does not claim to fix it or
establish the origin of every reported spacing event.

## 0.1.0 — 2026-08-11

- Initial published Android voice-input release.
