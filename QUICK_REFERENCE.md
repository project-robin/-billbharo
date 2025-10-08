# 🚀 Quick Reference - Gemini Voice Integration

## 📍 Key Files

| File | Purpose | Changes |
|------|---------|---------|
| `GeminiInvoiceParser.kt` | Gemini API wrapper | **NEW** - 137 lines |
| `NewInvoiceViewModel.kt` | Business logic | Modified `processVoiceInput()` (lines 233-297) |
| `NewInvoiceScreen.kt` | UI dialog | Added `initialQuantity`, `initialPrice` params |
| `build.gradle.kts` | Dependencies | Added Gemini SDK + BuildConfig |
| `local.properties` | API key | Add `gemini.api.key=YOUR_KEY` |

---

## 🔧 Setup (2 Minutes)

```bash
# 1. Get API Key
Open: https://aistudio.google.com/apikey

# 2. Add to local.properties
echo "gemini.api.key=YOUR_ACTUAL_KEY" >> local.properties

# 3. Sync Gradle
./gradlew build --dry-run
```

---

## 🎯 How It Works

```
User speaks → STT → Gemini API → JSON → Auto-fill form
                        ↓ fails
                    Regex Parser → Item name only
```

**Example:**
```kotlin
Input:  "do bread pachas rupay"
Output: {"item": "Bread", "quantity": 2, "price": 50, "confidence": 0.95}
Result: ✅ All fields auto-filled
```

---

## 🧪 Testing

```kotlin
// Test 1: With Gemini (high confidence)
speak("teen kilo aloo sau rupay")
// Expected: Item="Aloo", Qty="3", Price="100" ✅

// Test 2: Without Gemini (fallback)
disable_network()
speak("do maggi")
// Expected: Item="Maggi", Qty="", Price="" (manual entry)

// Test 3: Low confidence
speak("ek biscuit")
// Expected: Item="Biscuit", Qty="", Price="" (no price → low conf)
```

---

## 🔍 Debugging

```kotlin
// Check if Gemini is initialized
Log.d("Gemini", "API Key: ${BuildConfig.GEMINI_API_KEY.take(5)}...")

// Monitor parsing
viewModelScope.launch {
    val result = geminiInvoiceParser.parseInvoiceItem("test input")
    Log.d("Gemini", "Result: $result")
}
```

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| "Unresolved reference: BuildConfig" | Sync Gradle + Rebuild |
| Fields not auto-filling | Check API key in `local.properties` |
| Slow response | Normal - Gemini takes ~2s |
| Always falls back to regex | API key incorrect/missing |

---

## 📊 Code Metrics

- **Confidence Threshold**: ≥ 0.85 (adjust in `GeminiInvoiceParser.CONFIDENCE_THRESHOLD`)
- **Timeout**: 10 seconds (adjust in `TIMEOUT_MS`)
- **Model**: `gemini-2.0-flash-exp` (fastest, latest)
- **Temperature**: 0.1 (deterministic output)
- **Max Tokens**: 256 (small JSON response)

---

## 🔒 Security Checklist

- [ ] API key in `local.properties` (NOT in code)
- [ ] `local.properties` in `.gitignore`
- [ ] No API key logged to console
- [ ] Network calls have timeout
- [ ] Fallback works without API key

---

## 🎓 Learn More

- **Prompt Engineering**: See `buildPrompt()` in `GeminiInvoiceParser.kt`
- **Fallback Logic**: See `processVoiceInput()` in `NewInvoiceViewModel.kt`
- **Schema Design**: See `GeminiParseResult` data class

---

**Need help?** Check `IMPLEMENTATION_SUMMARY.md` for detailed docs.
