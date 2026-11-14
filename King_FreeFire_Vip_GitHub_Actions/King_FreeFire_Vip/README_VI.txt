KING_FREEFIRE_VIP - Android Studio project (Kotlin)

BAN CAU HINH
- Android Gradle Plugin: 8.7.3
- Kotlin Android plugin: 2.0.21
- Gradle Wrapper: 8.9 (duoc tao boi tools\prepare_gradle.bat)
- JDK: 17 hoac 21 (JBR 21.0.8 cua ban co the su dung)
- Android SDK Platform: 35

CAI DAT NHANH TREN WINDOWS
1. Giai nen ZIP vao mot thu muc co quyen ghi.
2. Dam bao may tinh co Internet.
3. Chay tools\prepare_gradle.bat (nhan chuot phai > Run as administrator chi neu Windows bao thieu quyen; thuong khong can).
   Script tai Gradle 8.9 tu services.gradle.org va tao cac file Gradle Wrapper chuan.
4. Mo Android Studio > Open > chon thu muc King_FreeFire_Vip.
5. Trong Settings > Build Tools > Gradle, chon Gradle distribution = Wrapper (recommended).
6. Chon Gradle JDK = JBR 21.0.8 hoac JDK 17/21.
7. Sync Project with Gradle Files.
8. Build > Build Bundle(s) / APK(s) > Build APK(s).

NEU SYNC VAN LOI
- Dam bao Gradle distribution dang la Wrapper, khong chon Gradle 9.x.
- Tat Offline mode neu dang bat.
- Gui phan Build Output co dong 'What went wrong' va 'Caused by'.

CAU HINH DU AN
- Android Gradle Plugin 8.7.3 tuong thich Gradle 8.9.
- Kotlin Android 2.0.21.
- compileSdk/targetSdk 35, minSdk 24.
- Dependencies duoc giu nguyen theo project goc.

KET NOI SERVER
- Dien thoai va may tinh chay KING LICENSE SERVER phai cung Wi-Fi/LAN.
- Trong app nhap IP LAN cua may tinh, vi du http://192.168.1.10:8000.
- Khong nhap 127.0.0.1 tren dien thoai: dia chi do tro ve chinh dien thoai.
- Mo cong 8000 trong Windows Firewall neu can.
- Server phai dang chay khi kich hoat hoac dong bo.

LUU Y
- Ma nguon Kotlin, giao dien, manifest va dependency cua ban duoc giu nguyen.
- ZIP nay la source project, khong phai APK da build/test tren thiet bi that.
- Script chi tai Gradle chinh thuc va tao wrapper; khong sua server hoac license.

TU DONG BUILD APK TREN GITHUB
- Workflow: .github/workflows/android-apk.yml
- Moi lan push code len nhanh nao, GitHub Actions se tu dong build APK Debug.
- Pull request va chay thu cong (Actions > Build Android APK > Run workflow) cung duoc ho tro.
- Vao repository > Actions > chon lan chay build thanh cong > Artifacts > tai KING_FREEFIRE_VIP-debug-apk.
- APK artifact duoc GitHub luu 14 ngay.
- Can bat GitHub Actions cho repository. Build can Internet de tai Gradle va dependencies.
- Workflow nay tao APK Debug de cai thu; chua ky APK Release de phat hanh.
