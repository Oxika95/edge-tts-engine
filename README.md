# Edge TTS Engine

Independent Android **system text-to-speech engine** that exposes Microsoft Edge neural voices to any app via the standard Android TTS API.

This project is **not affiliated with** Microsoft, [Readest](https://github.com/readest/readest), or [NekoSpeak](https://github.com/siva-sub/NekoSpeak).

## What it does

1. Install the APK.
2. Open **Settings → Accessibility → Text-to-speech output** (path varies by OEM) and select **Edge TTS Engine**.
3. Pick a language/voice. Any app that uses `TextToSpeech` can then speak with Edge voices.

Speech is synthesized over the network using Microsoft Edge’s public read-aloud endpoint. That API is **unofficial**, requires internet, and can break without notice.

## Origins

- **UI + Android TTS plumbing** derived from [NekoSpeak](https://github.com/siva-sub/NekoSpeak) (MIT). See [NOTICE](NOTICE).
- **Edge Speech client + voice catalog** ported from [Readest `edgeTTS.ts`](https://github.com/readest/readest/blob/main/apps/readest-app/src/libs/edgeTTS.ts) (AGPL-3.0).

The combined work is released under the **GNU Affero General Public License v3.0**. See [LICENSE](LICENSE).

## Build

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## License

AGPL-3.0. NekoSpeak-derived portions remain under MIT as noted in `NOTICE`.
