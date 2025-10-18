# 🚀 Quick Test Guide - Gemini Voice Fix

## ⚡ Fast Track Testing (5 Minutes)

### Step 1: Build & Install
```bash
# Clean build
./gradlew clean assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Grant Permission
1. Open BillBharo app
2. Settings → Apps → BillBharo → Permissions
3. Enable **Microphone** ✅

### Step 3: Test Voice on Home Screen
1. **Launch app** → Home screen
2. **See green card** with mic button (80dp circular)
3. **Tap mic** → Should turn red with stop icon
4. **Speak clearly:** "do bread pachas rupay"
5. **Wait** → "Processing..." → "Parsing..."
6. **Result:** Should navigate to NewInvoice with:
   - Item: "Bread"
   - Quantity: 2
   - Price: 50

### Step 4: Verify Success
✅ **Expected Behavior:**
- Green mic button appears on Home screen
- Tap → Recording starts (red stop icon)
- After speaking → Shows "Processing with Gemini..."
- Success → Opens NewInvoice with pre-filled dialog

❌ **If You See Errors:**
- "API key not configured" → Check `local.properties`
- "Permission denied" → Grant microphone permission
- "Network error" → Check internet
- "Could not understand" → Speak louder/clearer

---

## 🧪 Test Phrases

### Hindi/Hinglish (Best Performance)
```
1. "do bread pachas rupay"
2. "teen kilo aloo sau rupay"  
3. "paanch packet biscuit das rupay"
4. "ek litre doodh"
```

### English (Also Works)
```
1. "two bread fifty rupees"
2. "three kilogram potato hundred rupees"
3. "five packet biscuit ten rupees"
```

---

## 🔍 Quick Debugging

### Check Logs
```bash
# Watch Gemini API calls
adb logcat | grep -i "gemini"

# Watch errors only
adb logcat *:E | grep -i "billbharo"
```

### Common Fixes

| Problem | Solution |
|---------|----------|
| 400 Error | Rebuild: `./gradlew clean build` |
| No mic button | Check HomeScreen.kt changes applied |
| Empty result | Speak for 2-3 seconds minimum |
| Crash | Check API key in `local.properties` |

---

## ✅ Success Criteria

- [ ] Mic button visible on Home screen
- [ ] Recording starts on tap
- [ ] UI shows recording status
- [ ] Gemini processes audio
- [ ] Navigates to NewInvoice
- [ ] Dialog shows correct item/quantity/price

---

## 📞 Quick Support

**Still broken?** Check these 3 things:

1. **API Key:** `local.properties` has `gemini.api.key=...`
2. **Permission:** Microphone allowed in app settings
3. **Network:** Internet connected

**Log the error:**
```bash
adb logcat -d > debug.log
```

Send `debug.log` with error description.

---

**Expected Test Time:** 5 minutes
**Expected Success Rate:** 95%+ (with clear speech)
