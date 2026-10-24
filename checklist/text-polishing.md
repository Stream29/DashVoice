# Text Polishing

- Count only letters and digits when deciding whether to polish.
- Default the minimum effective character count to 20.
- Bypass the text model below the configured threshold.
- Keep the text-polishing prompt user-configurable on both platforms.
- Preserve user wording; correct only speech-recognition and punctuation-recognition errors.
- Use `qwen-flash` with `enable_thinking` disabled.
- Request a JSON object containing the final text.
- Serialize required request parameters explicitly, including default-valued
  `model`, `stream`, `temperature`, `enable_thinking`, and `response_format.type`.
- Keep existing transcript-formatting options after polishing.
- When CJK-space removal is enabled, remove gaps between Han characters
  without removing word spaces within English or Korean text.
- Fall back to the final ASR transcript on request or response failure.
