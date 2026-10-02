# French VO Expressif V3

Android 10+ app for expressive French narration through Gemini 3.8 Flash TTS. Includes Charon/Puck/Kore/Aoede, narration styles and inline direction tags. Internet and a user's Google AI Studio API key are required. Keys stay in memory only, are sent in an HTTPS request header to Google, and are not embedded, persisted or logged. The script is sent to Google on Generate. Free-tier quota depends on the user's Google project; use a project without billing to avoid paid usage. No automatic paid-model fallback.

Tags: [naturel] [énergique] [mystère] [chuchote] [surpris] [amusé] [sérieux] [calme] [pause]. Delivery tags become speech_metadata.style fields, and pause becomes <short pause>. Unknown tags are rejected. Maximum 30,000 characters, batched into up to 1,200 characters per request. Keep the app open. Long audio requires several requests and may exceed free quotas or vary across parts.

Generate → listen → save WAV to Downloads/FrenchVO. The app uses a separate application ID and can coexist with V2. It does not load the native Tom runtime that crashed on the user's phone. The cause of that V2 crash has not been established.

Validation: unit tests cover direction removal, long script preservation and WAV concatenation/mismatched-format rejection. Build validation does not establish successful live API access or narration quality; those need the user's key. No real API key is included in tests.

Official API contract: https://ai.google.dev/gemini-api/docs/generate-content/speech-generation
Pricing: https://ai.google.dev/gemini-api/docs/pricing
