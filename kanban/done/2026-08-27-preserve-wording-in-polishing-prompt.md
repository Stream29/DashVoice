# Task Tree

- Preserve wording in text-polishing prompts
  - [done] Update the shared default prompt
  - [done] Migrate unchanged Android defaults
  - [done] Install and verify the updated application

# Details

The prompt must preserve the user's wording and original phrasing. It may correct only speech-recognition and punctuation-recognition errors. Android configurations that still equal the previous default prompt are migrated to the revised default; user-edited prompts remain unchanged.

The Android Room database advances from version 6 to 7 even though its schema is unchanged, because the migration updates persisted default content conditionally.
