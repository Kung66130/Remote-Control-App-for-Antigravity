# 🚀 Antigravity Remote (Android)

<div align="center">

![Antigravity Remote](https://img.shields.io/badge/Platform-Android%20%7C%20Jetpack%20Compose-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Zero Install](https://img.shields.io/badge/PC%20Setup-Zero--Install-00C853?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**แอปพลิเคชันมือถือควบคุมและสั่งงาน Google Antigravity บนคอมพิวเตอร์ผ่านมือถือแบบ Real-time โดยไม่ต้องติดตั้งโปรแกรมหรือสคริปต์ใดๆ บนเครื่องคอมพิวเตอร์ (Zero-Install)**

[ฟีเจอร์เด่น](#-ฟีเจอร์เด่น-key-features) • [วิธีใช้งาน](#-วิธีเริ่มต้นใช้งาน-quick-start) • [สถาปัตยกรรม](#-สถาปัตยกรรมและเทคโนโลยี-architecture) • [การติดตั้งและคอมไพล์](#-วิธี-build--run-จาก-source-code)

</div>

---

## 🌟 ฟีเจอร์เด่น (Key Features)

- ⚡ **Zero-Install บนเครื่องคอมพิวเตอร์ (100% Zero Host Setup):**
  - ไม่ต้องรัน Python, ไม่ต้องเขียนสคริปต์ `.bat`, และไม่ต้องเปิดพอร์ต Firewall ใดๆ บนเครื่องคอมพิวเตอร์
  - ใช้ระบบ Remote Tunnel มาตรฐานของ Google Antigravity ในตัวทันที
- 📷 **ระบบสแกนความเร็วสูง (Google ML Kit Barcode Scanning):**
  - สแกน QR Code บนหน้าจอคอมพิวเตอร์ได้รวดเร็วระดับมิลลิวินาทีในแนวตั้ง (Portrait Viewfinder)
- 🔒 **ระบบจำเครื่องถาวร (One-Time Pairing):**
  - สแกนเชื่อมต่อแค่ครั้งแรกครั้งเดียว ตัวแอปจะจำเครื่องคอมพิวเตอร์ของคุณไว้ตลอดไป
  - เปิดแอปครั้งต่อไป เข้าสู่หน้าจอควบคุมสดได้ทันทีโดยไม่ต้องสแกนซ้ำ
- 📸 **รองรับการแนบและอัปโหลดรูปภาพ (Full Media & Photo Gallery Picker):**
  - รองรับการกดแนบภาพหน้าจอหรือรูปภาพจากแกลเลอรีในมือถือ เพื่อส่งให้ AI วิเคราะห์ปัญหาได้ทันที
- ⚡ **Live Real-Time Streaming:**
  - แสดงผลความคิดของ AI (Thinking stream), ตัวอักษรโค้ด, และการแก้ไขไฟล์แบบสดๆ พร้อมหน้าจอคอมพิวเตอร์
  - รองรับการกดยอมรับ / ปฏิเสธการรันคำสั่ง Terminal (Tool Approval) ได้จากทุกที่
- 🌙 **AMOLED Dark Modern UI:**
  - ดีไซน์สวยงามด้วย Jetpack Compose และ Material 3 พร้อมระบบป้องกันหน้าจอดับ (Keep Screen Awake)

---

## 🚀 วิธีเริ่มต้นใช้งาน (Quick Start)

### 1. บนโทรศัพท์มือถือ (Android)
1. ติดตั้งไฟล์ `Antigravity_Remote.apk` ลงในโทรศัพท์
2. เปิดแอปพลิเคชันขึ้นมา

### 2. บนเครื่องคอมพิวเตอร์ (PC / Mac / Linux)
1. เปิดโปรแกรม **Google Antigravity**
2. คลิกที่ไอคอน **⚙️ Settings** (มุมซ้ายล่าง) ➔ เลือกเมนู **Application**
3. เลื่อนลงมาที่หัวข้อ **Remote Control** และเปิดสวิตช์สีเขียว 🟢 **`Enable Remote Control`**
4. หน้าจอคอมพิวเตอร์จะแสดงภาพ **QR Code** ขึ้นมา

### 3. สแกนเพื่อเชื่อมต่อ
1. บนมือถือ แตะปุ่ม **`[📷 สแกน QR Code บนจอคอม]`**
2. ส่องกล้องไปที่ QR Code บนหน้าจอคอมพิวเตอร์
3. 🎉 **เริ่มสั่งงาน AI และติดตามการเขียนโค้ดได้ทันทีจากทุกที่ตลอด 24 ชั่วโมง!**

---

## 🏗️ สถาปัตยกรรมและเทคโนโลยี (Architecture & Tech Stack)

```text
┌─────────────────────────┐                     ┌─────────────────────────┐
│     Android Device      │                     │     Host Computer       │
│  (Antigravity Remote)   │                     │   (Google Antigravity)  │
│                         │                     │                         │
│  • Jetpack Compose M3   │                     │  • Built-in WebRTC /    │
│  • CameraX + ML Kit     │ ◄── Encrypted ────► │    gRPC Secure Tunnel   │
│  • WebChrome File Picker│      Cloud Stream   │  • Zero Software Setup  │
│  • Persistent Prefs     │                     │  • Direct Workspace Ops │
└─────────────────────────┘                     └─────────────────────────┘
```

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose, Material 3
- **Camera & QR:** AndroidX CameraX (`1.4.1`), Google ML Kit Barcode Scanning (`17.3.0`)
- **Networking & Integration:** Android WebView with customized `WebChromeClient` (`onShowFileChooser`) for photo gallery integration
- **Storage:** Android Encrypted/Private SharedPreferences for One-Time Pairing

---

## 🛠️ วิธี Build & Run จาก Source Code

### ความต้องการของระบบ:
- **Android Studio:** Koala / Ladybug หรือใหม่กว่า
- **JDK:** Java 17
- **Android SDK:** Compile SDK 36, Min SDK 24 (Android 7.0+)

### คำสั่งคอมไพล์ผ่าน Terminal:

```bash
# Clone repository
git clone https://github.com/Kung66130/Remote-Control-App-for-Antigravity.git
cd Remote-Control-App-for-Antigravity

# Compile Debug APK
./gradlew assembleDebug

# Install to connected Android device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
