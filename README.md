# Words in Memory

# User Stories:

[done] As a user I want to enter a foreign word or sentence with its translation.

[done] As a user I want the word I have enter to be cached in app.

[done] As a user I want in case of sentence app split it into words. 

[done] As a user I want app shows me this words or sentences on the dashboard. 

[done] As a user I do not want to see a word translation when I see this word. 

[done] As a user I want to see translation when I tap on the word. 

[done] As a user I want to mark word as completed when I read it without translation. 

[done] As a user I want to have completed words to be shadowed (less visible compare to not completed words).

[done] As a user I want to filter words by its flag "completed" or "uncompleted".

[done] As a user I want to have non default launcher icon. 

[done] As a user I want to modify translation of word or sentence.  

[done] As a user I want to have AI powered conversation with recently added words usage

# Publications

<a href="https://gelassen.github.io/2023/08/20/case-study-automatic-chinese-text-tokenisation-and-translation.html">Case study: automatic Chinese text word segmentation and translation</a>

<a href="https://gelassen.github.io/words-in-memory/">Promo page</a>

---

## Architecture (chat)

| Layer | Responsibility |
|-------|----------------|
| `ChatViewModel` | UI state, vocabulary snapshot, conversation history |
| `ChatRepository` | Builds chat-completions request, calls backend, maps errors |
| `LlmApi` + `LlmBaseUrlInterceptor` | Retrofit client; host/port from SharedPreferences |
| Settings | `llm_host`, `llm_port`, `llm_model` |

Swapping model or provider = change Settings (and run a compatible server). No code changes in UI.

---

## Host machine requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM | 12 GB (for 9B Q4) | 16 GB+ |
| Disk | ~10 GB free (model + Docker) | 20 GB+ |
| GPU | Optional (CUDA/ROCm) | NVIDIA 8 GB+ VRAM for comfortable speed |
| OS | Linux / Windows 10+ / macOS | Linux preferred |
| Docker | 24+ | Latest stable |
| Network | Phone and PC on the **same Wi‑Fi** | — |

CPU-only works; first replies from a 9B model may take 15–60 seconds.

---

## Deploy local model (host PC) — Ollama example

Ops detail only; the app does not hard-code this product name.

### 1. Start the server

```bash
docker run -d --name ollama \
  -p 0.0.0.0:11434:11434 \
  -v ollama:/root/.ollama \
  ollama/ollama:latest
```

Use `0.0.0.0` so a physical phone on the same LAN can connect.

### 2. Pull / verify the model

```bash
docker exec -it ollama ollama list
docker exec -it ollama ollama pull qwen3.5:9b
```

### 3. Health check

```bash
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3.5:9b",
    "messages": [
      {"role": "system", "content": "You are a short Chinese tutor. No reasoning."},
      {"role": "user", "content": "Say hello in one short sentence."}
    ],
    "temperature": 0.7,
    "max_tokens": 200,
    "reasoning_effort": "none"
  }'
```

### 4. PC IP for the phone

```bash
hostname -I | awk '{print $1}'
```

---

## Android app configuration

1. Copy the Kotlin / XML files from this folder into the app module.
2. In **Settings**:

| Preference | Emulator | Physical phone |
|------------|----------|----------------|
| AI server host | `10.0.2.2` | PC LAN IP, e.g. `192.168.1.42` |
| AI server port | `11434` | `11434` |
| AI model name | `qwen3.5:9b` | `qwen3.5:9b` |

3. Cleartext HTTP must be allowed (`android:usesCleartextTraffic="true"`).
4. Build & open **AI Practice** chat.

---

## Files in this patch

```
src/main/java/.../network/LlmModels.kt
src/main/java/.../network/LlmApi.kt
src/main/java/.../network/LlmBaseUrlInterceptor.kt
src/main/java/.../repository/ChatRepository.kt
src/main/java/.../ui/chat/ChatViewModel.kt
src/main/java/.../di/AppModule.kt
src/main/res/xml/preferences.xml
src/main/res/values/strings_llm.xml
README.md
```

Remove after merge: `GeminiApi`, `GeminiModels`, any old `Ollama*` sources, `gemini_api_key` preference.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Connection refused / timeout | Same Wi‑Fi? Server bound only to 127.0.0.1? Firewall? |
| HTTP 404 model | Model not installed; check **AI model name** in Settings |
| Empty assistant message | Backend may need `reasoning_effort: none` (already sent by `ChatRepository`) |
| Slow replies | GPU runtime, or a smaller model name in Settings |
| Emulator works, phone does not | Replace `10.0.2.2` with the real LAN IP of the PC |
