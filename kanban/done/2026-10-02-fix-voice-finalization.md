# Task Tree

- [done] `Confirm source, deployment identity, and rollback`()
- [done] `Fix the polishing request contract`()
- [done] `Normalize unwanted Chinese word spaces`()
- [done] `Run regression tests and build the APK`()
- [done] `Update and validate both phones`()
  - [done] `Update and validate marble`()
  - [done] `Update and validate prague after handback`()
- [done] `Restore diagnostic settings and remove test hosts`()
  - [done] `Remove marble test hosts and verify effective volume`()
  - [done] `Restore prague verbose logging and remove its test host`()

# Details

- User explicitly requested fixing DashVoice and updating both phones.
- Existing APKs were backed up outside Git before deployment.
- Fixed the confirmed HTTP 400 defect: required default-valued polishing
  parameters are now explicitly serialized, including nested JSON format.
  The shared ASR serializer remains unchanged.
- Fixed the Han/Han formatting gap, including repeated and Unicode spaces.
  English and Korean word spacing and the formatting switch are preserved.
- Scope: fix these demonstrated defects and validate return to Fcitx.
  The intermittent unavailable-engine symptom has not been reproduced; do not
  claim it fixed or deploy speculative Fcitx modifications.
- No settings/database migration, uninstallation of DashVoice, credential
  export, Git commit, or push is needed.
- Deployment backups and rollback notes belong under
  `~/.local/state/DeviceManagement/updates/2026-10-02-dashvoice/`, not Git.
- IDEA MCP was unavailable and no local project IDE was identified.
- Initial offline build failed because pinned artifacts were not cached.
  The successful retry reused the project's Java 17 daemon with one worker;
  the other daemon was untouched.
- A successful-polishing log marker contains no transcript or credential data.
- Java 17 daemon reuse succeeded; debug unit tests and release assembly passed.
  All 33 tests passed with zero failures, errors, or skips; release vital lint
  also passed as part of assembly.
- `marble` was updated in place to `0.1.1`; matching certificate verified,
  settings preserved, Android default IME unchanged. Its microphone permission
  was originally ungranted and was granted for the requested voice repair.
- On `marble`, a repeated
  [public Chinese sample](https://huggingface.co/FunAudioLLM/SenseVoiceSmall/tree/main/example)
  triggered successful polishing at 17:26:59.630. Final text had 38 characters,
  zero whitespace, and no composing span. No missing-model fallback occurred.
- Soft-keyboard `nihao` committed Chinese successfully after both voice
  submission and cancellation. This is regression evidence, not proof that
  the intermittent unavailable-engine defect is fixed.
- Both temporary test hosts were removed from `marble`; its original English
  engine was reselected, media speaker volume returned to 0/150, and the
  phone returned to the launcher. The ROM's internal muted flag became true
  at zero volume and ignored unmute requests; the rollback notes record this
  difference. No audio/system service was restarted to reset it.
- Diagnostic collectors ended. Temporary sample, test-host builds, APKs,
  screenshots, and `marble`'s phone UI dump were removed.
- `prague` was paused when the user took over and disconnected ADB.
- On 2026-10-03 the user reconnected and handed `prague` back. Its in-place
  `0.1.1` update succeeded with the original microphone grant retained.
  Polishing completed at 02:53:07.994; final text had 41 characters, zero
  whitespace, and no composing span. Chinese soft-keyboard input worked
  after both submission and cancellation.
- Its installed APK was pulled back and verified byte-identical to the
  signed update used on `marble`. Both deployments are `0.1.1`, version code
  `1`; this sideload-only code preserves in-place rollback without data loss.
- `prague` verbose logging was restored to false without restarting Fcitx
  (PID 15438 unchanged). Its original test host was already absent; the new
  synthetic test host and UI dump were removed. Media speaker volume and
  muted state stayed 30/150 and false; the phone returned to the launcher.
- Restricted APK backups, signed update, and sanitized validation summary:
  `~/.local/state/DeviceManagement/updates/2026-10-02-dashvoice/`.
