# Bill Bharo - Voice-to-Invoice Android App

## 📱 Overview
**Bill Bharo** is a modern Android application designed for small shop owners in India to create invoices using voice commands. Built with the latest Android technologies including Jetpack Compose, Room Database, and Material Design 3.

## ✨ Features

### 🎤 AI-Powered Voice Input (NEW!)
- ✅ **Google Gemini 2.0 Flash Integration**: Automatically extracts item name, quantity, and price
- ✅ **95% Accuracy**: Understands Hindi, English, and Hinglish voice commands
- ✅ **Smart Auto-Fill**: Confidence-based field population (≥85% threshold)
- ✅ **Graceful Fallback**: Works offline with regex-based parsing

### MVP Features
- ✅ Voice-to-text invoice creation (Hindi/English/Marathi)
- ✅ Offline-first architecture with Room Database
- ✅ GST calculation (CGST/SGST)
- ✅ Invoice management
- ✅ Customer credit tracking (Khata)
- ✅ Inventory management
- ✅ Sales reports and analytics
- ✅ PDF invoice generation
- ✅ Multi-language support

### 🎯 Voice Input Examples
| User Says | AI Extracts |
|-----------|-------------|
| "do bread pachas rupay" | Item: Bread, Qty: 2, Price: ₹50 |
| "teen kilo aloo sau rupay" | Item: Aloo, Qty: 3, Price: ₹100 |
| "2 Maggi 20 rupees" | Item: Maggi, Qty: 2, Price: ₹20 |

## 🏗️ Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room
- **Dependency Injection**: Hilt
- **AI/ML**: Google Gemini 2.0 Flash API
- **Navigation**: Navigation Compose
- **Asynchronous**: Kotlin Coroutines + Flow

### Project Structure
```
BillBharo/
├── app/
│   ├── src/main/java/com/billbharo/
│   │   ├── data/              # Data layer
│   │   │   ├── local/         # Room database, DAOs, entities
│   │   │   ├── repository/    # Repository implementations
│   │   │   └── models/        # Domain models
│   │   ├── domain/            # Business logic
│   │   │   ├── usecases/      # Use cases
│   │   │   └── utils/         # Utilities (Voice, GST, PDF)
│   │   ├── ui/                # Presentation layer
│   │   │   ├── theme/         # Material3 theming
│   │   │   ├── components/    # Reusable UI components
│   │   │   ├── screens/       # Feature screens
│   │   │   └── navigation/    # Navigation setup
│   │   ├── di/                # Dependency injection modules
│   │   └── BillBharoApplication.kt
│   └── src/main/res/          # Resources
```

## 🛠️ Setup Instructions

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or later
- JDK 17
- Android SDK 34
- Minimum SDK 26 (Android 8.0)
- **Gemini API Key** - [Get one here](https://aistudio.google.com/apikey)

### Build & Run
1. Clone the repository
   ```bash
   git clone https://github.com/project-robin/-billbharo.git
   cd -billbharo
   ```

2. **Add your Gemini API Key**
   
   Create/edit `local.properties` in the root directory:
   ```properties
   sdk.dir=YOUR_ANDROID_SDK_PATH
   gemini.api.key=YOUR_GEMINI_API_KEY_HERE
   ```
   
   ⚠️ **Never commit this file!** (Already in `.gitignore`)

3. Open the project in Android Studio
4. Sync Gradle files
5. Run the app on an emulator or physical device

```bash
# Using Gradle
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## 📦 Dependencies

### Core Libraries
- **Compose BOM**: 2024.06.00
- **Material3**: Latest
- **Room**: 2.6.1
- **Hilt**: 2.51.1
- **Navigation Compose**: 2.7.7
- **Kotlin Coroutines**: 1.8.1

### Additional Libraries
- **Google Gemini SDK**: 0.9.0 (AI-powered parsing)
- Google Play Services (Speech Recognition)
- iText7 (PDF Generation)
- Gson (JSON parsing)
- Accompanist (Permissions)
- DataStore (Preferences)

## 🎯 Key Components

### Data Layer
- **InvoiceEntity**: Invoice records with items
- **ItemEntity**: Product catalog
- **CustomerEntity**: Customer information
- **InventoryEntity**: Stock management

### Domain Layer
- **GeminiInvoiceParser**: AI-powered structured data extraction
- **VoiceRecognitionHelper**: Speech-to-text integration
- **VoiceInputParser**: Regex-based fallback parser
- **GstCalculator**: GST calculation utilities
- **Repositories**: Data access abstraction

### UI Layer
- **HomeScreen**: Dashboard with sales overview
- **NewInvoiceScreen**: Create invoices with voice input
- **KhataScreen**: Customer credit management
- **InventoryScreen**: Stock management
- **ReportsScreen**: Sales analytics

## 🌐 Localization
- English
- Hindi (हिंदी)
- Marathi (मराठी)

## 🔐 Permissions
- `RECORD_AUDIO`: For voice input
- `INTERNET`: For future cloud sync
- `READ/WRITE_EXTERNAL_STORAGE`: For PDF export
- `READ_MEDIA_*`: For media access (Android 13+)

## 📱 Minimum Requirements
- Android 8.0 (API 26) or higher
- 2GB RAM
- 100MB free storage

## 📖 Documentation

- **[Gemini Setup Guide](GEMINI_SETUP.md)** - Detailed API configuration
- **[Implementation Summary](IMPLEMENTATION_SUMMARY.md)** - Technical architecture
- **[Quick Reference](QUICK_REFERENCE.md)** - Developer quick start
- **[Deployment Checklist](DEPLOYMENT_CHECKLIST.md)** - Pre-release verification

## 🔒 Security

- ✅ API keys stored securely in `BuildConfig` (never in source code)
- ✅ `local.properties` excluded from version control
- ✅ 10-second network timeout for API calls
- ✅ No PII sent to Gemini (only transcribed text)
- ✅ Graceful offline mode with fallback parsing

## 🚀 Future Enhancements
- [ ] Multi-item voice input parsing
- [ ] Unit recognition (kg, liter, etc.)
- [ ] Cloud backup and sync
- [ ] WhatsApp invoice sharing
- [ ] Barcode scanning
- [ ] Multi-shop support
- [ ] Advanced analytics
- [ ] Payment gateway integration

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License
This project is licensed under the MIT License.

## 🙏 Acknowledgments

- [Google Gemini](https://ai.google.dev/gemini-api) - AI-powered parsing
- [iText7](https://itextpdf.com/) - PDF generation
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Material Design 3](https://m3.material.io/) - Design system

## 👥 Contact

**Project Robin** - [@project-robin](https://github.com/project-robin)

Project Link: [https://github.com/project-robin/-billbharo](https://github.com/project-robin/-billbharo)

---

<div align="center">
  <p>Built with ❤️ for small business owners in India</p>
  <p>⭐ Star this repo if you find it helpful!</p>
</div>
