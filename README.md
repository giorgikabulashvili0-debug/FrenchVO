# French VO V2 — Android

Bundled French male Tom voice runs offline through Sherpa ONNX. Android French system voices remain available. WAV export goes to Downloads/FrenchVO.

Tom input limit: 30,000 UTF-16 characters including spaces. Long narration is split into <=600-character parts and streamed to one 16-bit mono 44.1 kHz WAV. System voices retain their engine limit, normally 4,000 characters. Keep the app open during synthesis; Stop cancels a job and discards unfinished exports.

Requires Android 10+ and arm64-v8a. First use prepares voice files in private storage. App is larger because it includes the model and native runtime. No paid API or account required.

## Build

GitHub Actions downloads the pinned Sherpa 1.13.8 runtime and the upstream Tom model, includes license notices, tests text splitting and French audio synthesis, and builds the debug APK with Gradle 8.9 / Java 17 / Android SDK 35.

Download FrenchVO-V2-APK in Actions. V1 was signed with a temporary debug key; uninstall V1 before installing V2 if Android reports a signature mismatch. Audio files already exported to Downloads remain accessible. The V2 signing key is cached for future builds.

## Third-party source and notices

See app/src/main/assets/NOTICES.txt. Tom model/dataset is credited to Tjiho/Piper, Sherpa ONNX to k2-fsa, and phonemization to espeak-ng. This app's source is available under AGPL-3.0-or-later; bundled components retain their licenses. The workflow embeds corresponding license texts into the APK.
