# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

**Bill Bharo** is a voice-to-invoice Android application built for small shop owners in India. It uses AI-powered voice recognition (Google Gemini 2.0 Flash) to create invoices from Hindi/English/Hinglish voice commands, handling GST calculations, customer credit tracking (Khata), and PDF generation.

**Tech Stack:**
- Kotlin, Jetpack Compose (Material 3)
- MVVM + Clean Architecture
- Room Database (offline-first)
- Hilt (Dependency Injection)
- Google Gemini 2.0 Flash API (AI voice parsing)
- iText7 (PDF generation)

---

## Essential Commands

### Build & Run
```powershell
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Full rebuild
./gradlew clean assembleDebug
```

### Testing
```powershell
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.billbharo.domain.utils.GeminiInvoiceParserTest"

# Run with verbose logging
./gradlew test --info

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```

### Development Workflow
```powershell
# Check for dependency updates
./gradlew dependencyUpdates

# Validate Gradle configuration
./gradlew build --dry-run

# Sync Gradle files (if Android Studio sync fails)
./gradlew --refresh-dependencies
```

---

## Critical Setup: Gemini API Key

**REQUIRED BEFORE FIRST BUILD:**

1. Get API key from https://aistudio.google.com/apikey
2. Create/edit `local.properties` in project root:
   ```properties
   sdk.dir=YOUR_ANDROID_SDK_PATH
   gemini.api.key=YOUR_GEMINI_API_KEY_HERE
   ```
3. Sync Gradle files

**Without this setup, the app will throw `IllegalStateException` at runtime.**

The app is currently in "PURE AI MODE" - it requires Gemini API to function. If the API key is missing or invalid, voice input features will fail.

---

## Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose Screens)             │
│  - HomeScreen, NewInvoiceScreen, etc.   │
│  - ViewModels handle UI state           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Domain Layer (Business Logic)          │
│  - UseCases (future)                    │
│  - Utils: GeminiInvoiceParser,          │
│    VoiceRecognitionHelper, GstCalculator│
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│  Data Layer (Persistence)                │
│  - Room Database (offline-first)        │
│  - Repositories (CustomerRepository,    │
│    InvoiceRepository, ItemRepository)   │
└─────────────────────────────────────────┘
```

### Key Architectural Patterns

1. **Offline-First**: Room Database as single source of truth; all operations work without internet
2. **Dependency Injection**: Hilt provides ViewModels, Repositories, and utility classes
3. **State Management**: UI state flows from ViewModels to Composables via StateFlow/State
4. **Navigation**: Single-activity architecture with Navigation Compose; routes defined in `NavGraph.kt`

### Critical Components

#### Voice Recognition Flow
```
User Voice → AudioRecorder (raw PCM capture, 16kHz mono)
  → GeminiAudioTranscriber (audio → text via Gemini 2.0 Flash)
  → GeminiInvoiceParser (text → structured JSON)
  → ViewModel (state update)
  → UI auto-fill
```

**Location**: `domain/utils/AudioRecorder.kt`, `domain/utils/GeminiAudioTranscriber.kt`, `domain/utils/GeminiInvoiceParser.kt`

**Key Behavior**:
- **100% GEMINI-POWERED**: NO Android SpeechRecognizer used - only Gemini 2.0 Flash for audio transcription
- Two-stage AI processing: audio→text (Gemini), text→JSON (Gemini)
- Supports Hindi, English, Marathi, and Hinglish with context-aware prompts
- 30-second timeout on transcription, 15-second timeout on parsing
- See `GEMINI_VOICE_TRANSCRIPTION.md` for complete architecture

#### Data Models & Relationships

**Core Entity**: `InvoiceEntity` (Room database)
- Has many `InvoiceItemEntity` (embedded list)
- Links to `CustomerEntity` (optional, for Khata feature)
- Contains GST calculations (CGST/SGST)

**Location**: `data/local/BillBharoDatabase.kt`, `data/models/Invoice.kt`

#### GST Calculation
- Fixed 18% GST rate (9% CGST + 9% SGST)
- Calculation logic in `domain/utils/GstCalculator.kt`
- Applied on subtotal before final amount computation

#### PDF Generation
- Uses iText7 library
- Generates invoices with Hindi/English support
- Saves to app-specific storage (Android 10+ scoped storage)
- **Location**: `domain/utils/PdfGenerator.kt`

---

## Code Structure

```
app/src/main/java/com/billbharo/
├── BillBharoApplication.kt        # Hilt entry point
├── MainActivity.kt                # Single activity host
├── data/
│   ├── local/
│   │   └── BillBharoDatabase.kt   # Room database definition
│   ├── models/
│   │   └── Invoice.kt             # Data models (Invoice, InvoiceItem)
│   ├── preferences/
│   │   └── PreferencesManager.kt  # DataStore preferences
│   └── repository/
│       ├── InvoiceRepository.kt   # Invoice CRUD operations
│       ├── ItemRepository.kt      # Item catalog management
│       └── CustomerRepository.kt  # Customer/Khata operations
├── di/
│   ├── AppModule.kt               # App-level DI providers
│   └── DatabaseModule.kt          # Database DI providers
├── domain/
│   └── utils/
│       ├── AudioRecorder.kt            # Raw PCM audio capture (16kHz mono)
│       ├── GeminiAudioTranscriber.kt   # Gemini audio-to-text transcription (CRITICAL)
│       ├── GeminiInvoiceParser.kt      # Gemini text-to-JSON parsing (CRITICAL)
│       ├── VoiceInputParser.kt         # Legacy regex parser (unused)
│       ├── GstCalculator.kt            # GST computation
│       ├── PdfGenerator.kt             # Invoice PDF creation
│       ├── ShareHelper.kt              # WhatsApp/social sharing
│       └── LocaleHelper.kt             # Language switching
└── ui/
    ├── navigation/
    │   └── NavGraph.kt            # Navigation routes & composable mapping
    ├── theme/
    │   ├── Color.kt               # MD3 color palette (see desgin_system.md)
    │   ├── Theme.kt               # App theme configuration
    │   └── Type.kt                # Typography definitions
    └── screens/
        ├── home/HomeScreen.kt            # Dashboard with sales overview
        ├── newinvoice/NewInvoiceScreen.kt # Invoice creation with voice
        ├── khata/KhataScreen.kt          # Customer credit tracking
        ├── inventory/InventoryScreen.kt  # Stock management
        ├── reports/ReportsScreen.kt      # Analytics
        └── settings/SettingsScreen.kt    # App preferences
```

---

## Common Development Tasks

### Adding a New Screen

1. Create screen file in `ui/screens/<feature>/NewScreen.kt`
2. Add route to `ui/navigation/NavGraph.kt`:
   ```kotlin
   sealed class Screen(val route: String) {
       object NewFeature : Screen("new_feature")
   }
   
   composable(Screen.NewFeature.route) {
       NewFeatureScreen(navController = navController)
   }
   ```
3. Create ViewModel if needed (annotate with `@HiltViewModel`)
4. Follow Material 3 design tokens from `desgin_system.md`

### Modifying Voice Recognition

**CRITICAL**: Uses ONLY Gemini 2.0 Flash - NO Android SpeechRecognizer.

**Files to modify**:
1. `domain/utils/GeminiAudioTranscriber.kt` - Audio transcription prompt engineering
2. `domain/utils/GeminiInvoiceParser.kt` - Structured data extraction prompt engineering
3. `ui/screens/newinvoice/NewInvoiceViewModel.kt` - Voice flow orchestration
4. `ui/screens/newinvoice/NewInvoiceScreen.kt` - UI handling

**Architecture notes**:
- Two-stage Gemini processing: (1) audio→text, (2) text→JSON
- AudioRecorder captures raw PCM at 16kHz, mono, 16-bit
- Transcription prompt in `buildTranscriptionPrompt()` (GeminiAudioTranscriber)
- Parsing prompt in `buildPrompt()` (GeminiInvoiceParser)
- See `GEMINI_VOICE_TRANSCRIPTION.md` for complete flow

### Database Schema Changes

1. Update entity classes in `data/local/BillBharoDatabase.kt`
2. Increment database version number
3. Add migration strategy:
   ```kotlin
   val MIGRATION_X_Y = object : Migration(X, Y) {
       override fun migrate(database: SupportSQLiteDatabase) {
           // SQL migration queries
       }
   }
   ```
4. Run app to execute migration

### Adding Dependencies

1. Edit `gradle/libs.versions.toml`:
   ```toml
   [versions]
   new-lib = "1.0.0"
   
   [libraries]
   new-lib = { group = "com.example", name = "library", version.ref = "new-lib" }
   ```
2. Add to `app/build.gradle.kts`:
   ```kotlin
   implementation(libs.new.lib)
   ```
3. Sync Gradle

---

## Design System (Material 3)

**Primary Colors**:
- Primary: `#1B5E20` (Dark Green - trust, money)
- Secondary: `#FF6F00` (Orange - energy, action)

**Typography**:
- Default: Roboto (Latin)
- Devanagari: Noto Sans Devanagari (+2sp for Hindi/Marathi)

**Key Spacing**:
- Base unit: 8dp
- Screen padding: 16dp (horizontal), 12dp (vertical)
- Corner radius: Small (8dp), Medium (12dp), Large (28dp)

**See `desgin_system.md` for complete design tokens.**

---

## Testing Strategy

### Unit Tests
- Focus on business logic: `GeminiInvoiceParser`, `GstCalculator`, `VoiceInputParser`
- Mock Gemini API responses for deterministic tests
- Example: Test confidence threshold handling, JSON parsing edge cases

### Integration Tests
- Test Room database migrations
- Test Repository + DAO interactions
- Mock external dependencies (Gemini API, STT)

### UI Tests (Compose)
- Test screen navigation
- Test form validation
- Test voice input dialog states

### Manual Testing Checklist
See `DEPLOYMENT_CHECKLIST.md` for pre-release verification steps.

---

## Known Issues & Limitations

1. **Single Item Parsing**: Voice input handles one item per utterance (multi-item support planned)
2. **Internet Required**: Audio transcription requires internet (Gemini API); completely offline-dependent
3. **No Android STT**: Removed Android SpeechRecognizer entirely; 100% Gemini-powered
4. **Audio Size**: Sends raw PCM audio (large); future: compress to Opus/AAC
5. **Unit Extraction**: Units (kg, liter) extracted but not validated against item catalog
6. **Language Detection**: Assumes mixed Hindi/English; doesn't auto-detect language
7. **PDF Hindi Support**: Some Hindi characters may not render correctly in PDFs (iText7 limitation)

---

## Security & Privacy

- **API Key**: Stored in `BuildConfig.GEMINI_API_KEY` (never hardcoded)
- **No Cloud Storage**: All data stored locally in Room Database
- **Voice Data**: Only transcribed text sent to Gemini (not audio files)
- **Permissions**: `RECORD_AUDIO` (for voice), `WRITE_EXTERNAL_STORAGE` (for PDF export)

---

## Build Configuration

### Variants
- **Debug**: MinifyEnabled = false, debugging enabled
- **Release**: MinifyEnabled = true, ProGuard rules applied

### ProGuard Rules
Located in `app/proguard-rules.pro`:
- Keep Hilt-generated classes
- Keep Room entities
- Keep Gemini SDK models
- Keep iText7 PDF classes

### Version Management
- **versionCode**: Integer (increment for each release)
- **versionName**: Semantic versioning (e.g., "1.0.0")
- Defined in `app/build.gradle.kts`

---

## Troubleshooting

### Build Fails with "Unresolved reference: BuildConfig"
**Solution**: Sync Gradle, clean build, rebuild project

### Voice Input Not Working
**Checks**:
1. Verify `gemini.api.key` in `local.properties`
2. Check internet connection (required for Gemini API)
3. Check logs for `GeminiParsingException` details

### Room Database Migration Errors
**Solution**: Uninstall app, or add proper migration strategy

### Compose Preview Not Rendering
**Solution**: Invalidate caches (`File > Invalidate Caches`), rebuild

---

## References

- **Gemini Setup**: `GEMINI_SETUP.md`
- **Voice Transcription Architecture**: `GEMINI_VOICE_TRANSCRIPTION.md` (NEW - READ THIS!)
- **Implementation Details**: `IMPLEMENTATION_SUMMARY.md`
- **Quick Start**: `QUICK_REFERENCE.md`
- **Pre-Release**: `DEPLOYMENT_CHECKLIST.md`
- **Design System**: `desgin_system.md`
- **Current Mode**: `PURE_AI_MODE.md`

---

## Language Support

The app supports three languages:
- **English** (default)
- **Hindi** (हिंदी)
- **Marathi** (मराठी)

Language switching handled by `LocaleHelper.kt`. Voice input supports all three languages + Hinglish (mixed).

---

## Notes for Future Developers

1. **Gemini API Key is MANDATORY** - App will crash without it; 100% Gemini-powered voice transcription
2. **NO Android SpeechRecognizer** - Removed completely; only Gemini 2.0 Flash for audio→text
3. **Internet Required for Voice** - Audio transcription needs network; consider UI messaging
4. **Follow Clean Architecture** - Keep layers separated; avoid UI logic in ViewModels
5. **Material 3 Guidelines** - Use design tokens from `desgin_system.md`
6. **Offline-First (Except Voice)** - Room Database is source of truth; voice needs internet
7. **Voice Input is Core Feature** - Test thoroughly with Hindi/English/Hinglish phrases
8. **GST Calculation** - Currently hardcoded to 18%; may need configuration for other regions
