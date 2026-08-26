# Task Tree

- Add length-gated transcript polishing
  - [done] Define shared threshold behavior
  - [done] Add Android configuration and routing
  - [done] Add desktop provider routing
  - [done] Validate Android and desktop paths

# Details

Use the final ASR transcript's effective character count, excluding whitespace and punctuation. The default threshold is 20 and is configurable on both platforms. Inputs shorter than the threshold bypass the text model. Longer inputs use DashScope `qwen-flash` with non-thinking structured output. Transport or response failures fall back to the ASR transcript.

Android persists the threshold with Room and applies the route inside the recognition service before emitting final results. The desktop provider keeps VInput's raw scene and performs the same route before emitting its final event.
