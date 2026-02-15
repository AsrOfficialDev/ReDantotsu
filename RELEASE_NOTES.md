# 🎉 ReDantotsu v1.0.5

This release focuses on improving build stability and aligning terminal-based environment configurations for a smoother development and release workflow.

## ✨ New Features & Fixes
* **Build Stability** – Increased Gradle and Kotlin daemon memory allocation to 4GB, effectively preventing OutOfMemory (OOM) errors during resource-intensive compilation and dexing phases.
* **Release Consistency** – Synchronized versioning to v1.0.5 across both Google and F-Droid flavors for better tracking.

## 🔧 Internal Changes
* **Memory Optimization** – Refined JVM arguments in `gradle.properties` to better manage native memory usage during builds.
* **Environment Fix** – Resolved `JAVA_HOME` discrepancies to ensure build tools point to compatible JDK 17 installations.

---

## 📥 Downloads

| Architecture | File |
| :--- | :--- |
| **Universal** | `ReDantotsu-universal-release.apk` |
| **ARM64** | `ReDantotsu-arm64-v8a-release.apk` |
| **ARMv7** | `ReDantotsu-armeabi-v7a-release.apk` |

> [!TIP]
> If you are building from source, ensure your `gradle.properties` has at least 4GB of memory assigned to the daemon to avoid build failures.

---

## ⚠️ Disclaimer
* ReDantotsu **does not host any content**. All streaming sources come from 3rd party extensions.
* ReDantotsu is **not affiliated** with AniList, MyAnimeList, or any content providers.
