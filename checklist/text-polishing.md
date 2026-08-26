# Text Polishing

- Count only letters and digits when deciding whether to polish.
- Default the minimum effective character count to 20.
- Bypass the text model below the configured threshold.
- Keep the text-polishing prompt user-configurable on both platforms.
- Use `qwen-flash` with `enable_thinking` disabled.
- Request a JSON object containing the final text.
- Keep existing transcript-formatting options after polishing.
- Fall back to the final ASR transcript on request or response failure.
