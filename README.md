# ResumeMatch V3 - AI Career Agent 🚀

ResumeMatch V3 is a full-stack, AI-powered Android application built to help users seamlessly generate ATS-friendly and Modern resumes. It features a conversational voice agent that extracts career details and securely stores them in a local Master Vault.

### ✨ Key Features
* **AI Voice Assistant:** Speak naturally about your experience; the app uses Google's Gemini LLM to parse and extract your data into structured JSON.
* **The Master Vault:** A crash-proof, dynamic local database that securely stores your Skills, Projects, and Work Experience.
* **Smart PDF Generation:** Dynamically injects your saved Vault data into professional PDF templates directly on the device.
* **Cloud-Connected:** Communicates seamlessly with a custom-built Python/FastAPI backend hosted on Render.

### 🛠️ Tech Stack
* **Frontend:** Kotlin, Jetpack Compose, Material 3
* **Local Storage:** Android Preferences DataStore, Gson
* **Network/API:** Retrofit, OkHttp
* **Backend:** Python, FastAPI, Uvicorn, Google GenAI (Gemini)
* **Cloud Hosting:** Render

### 📥 Installation
You can download the latest production-signed APK from the [Releases Tab](https://github.com/rehanu04/resumematch-v3-android/releases).
*(Note: If you receive a "Play Protect" warning, simply tap "More details" -> "Install anyway" as this app is sideloaded outside of the Google Play Store).*
