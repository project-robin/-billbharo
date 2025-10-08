# 🔧 Gemini AI Integration Setup

## Overview
This app now uses **Google Gemini 2.0 Flash** to intelligently parse voice input and auto-fill invoice fields (item, quantity, price) with high accuracy.

---

## 🔑 Step 1: Get Your Gemini API Key

1. Visit: **https://aistudio.google.com/apikey**
2. Click **"Get API Key"** or **"Create API Key"**
3. Copy your API key (starts with `AIza...`)

---

## 📝 Step 2: Add API Key to `local.properties`

Open `local.properties` in your project root and add:

```properties
gemini.api.key=YOUR_ACTUAL_API_KEY_HERE
```

**Example:**
```properties
sdk.dir=D:\\ai coding tools\\android studio\\sdk&ndk
gemini.api.key=AIzaSyABcDeFgHiJkLmNoPqRsTuVwXyZ1234567
```

⚠️ **NEVER commit this file to version control!** (Already in `.gitignore`)

---

## 🏗️ Step 3: Sync & Build

1. Click **"Sync Now"** in Android Studio
2. Clean build: `Build → Clean Project`
3. Rebuild: `Build → Rebuild Project`

---

## ✅ How It Works

### Architecture:
```
Voice Input → STT (SpeechRecognizer) → Gemini 2.0 Flash → Structured JSON → Auto-fill Form
                                               ↓ (if fails)
                                         Regex Parser (fallback)
```

### Confidence Threshold:
- Fields are **only auto-filled** if Gemini returns confidence ≥ **0.85**
- Low confidence results fall back to manual entry

### Example Inputs:
| User Says                | Gemini Extracts                                      |
|-------------------------|------------------------------------------------------|
| "do bread pachas rupay" | Item: Bread, Qty: 2, Price: ₹50, Confidence: 0.95  |
| "teen kilo aloo sau"    | Item: Aloo, Qty: 3, Price: ₹100, Confidence: 0.90  |
| "2 Maggi 20 rupees"     | Item: Maggi, Qty: 2, Price: ₹20, Confidence: 0.98  |

---

## 🔒 Security Notes

1. **API Key Storage**: Securely loaded from `BuildConfig.GEMINI_API_KEY`
2. **No Hardcoding**: Key is never in source code
3. **Network Timeout**: 10-second limit prevents hanging
4. **Offline Fallback**: Works without internet (uses regex parser)

---

## 🧪 Testing Without API Key

The app works **without** Gemini:
- If no API key is set, it falls back to the existing regex parser
- All features remain functional (just less accurate)

---

## 📚 API Documentation

- **Gemini API**: https://ai.google.dev/gemini-api/docs
- **Android SDK**: https://github.com/google/generative-ai-android
- **Model Used**: `gemini-2.0-flash-exp` (fastest, latest)

---

## 🐛 Troubleshooting

### Build Error: "Unresolved reference: BuildConfig"
**Fix:** Sync Gradle files and rebuild.

### "API key not configured" at runtime
**Fix:** Check `local.properties` has the correct key format.

### Gemini returns low confidence
**Fix:** Speak more clearly or use simpler phrases (e.g., "2 bread 50 rupees").

---

## 🎯 Future Enhancements

- [ ] Support multiple items in one utterance
- [ ] Add unit recognition (kg, liter, etc.)
- [ ] Cache common items for offline mode
- [ ] Add voice feedback confirmation

---

**Need help?** Check `GeminiInvoiceParser.kt` for implementation details.
