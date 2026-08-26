# Task Tree

- Make text-polishing prompts configurable
  - [done] Define Android persistence and settings behavior
  - [done] Add desktop provider configuration
  - [done] Validate and publish Android changes

# Details

Persist the prompt in Android Room and expose it as an automatically saved multiline setting. Read the desktop prompt from the VInput provider environment. Both platforms default to the existing structured-output prompt and send the configured prompt as the system message.

The Android database advances from version 5 to 6. Empty prompts are rejected by the existing settings validation path so a configured polish route always has an instruction.
