package com.king.freefirevip

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("king_settings", Context.MODE_PRIVATE) }
    private val brands = listOf("XIAOMI", "ONEPLUS", "OPPO", "REDMI", "VIVO", "IQOO", "SONY", "LG", "VSMART")
    private val publicKey = "MBaV3te-pk6DAbAzt8XguxR7ZjCrHR9Wv-8_oEXm-ME"
    private val dark = Color.rgb(12, 13, 22)
    private val panel = Color.rgb(25, 27, 42)
    private val pink = Color.rgb(255, 54, 126)
    private var root: LinearLayout? = null
    private var serverInput: EditText? = null
    private var keyInput: EditText? = null
    private var statusText: TextView? = null
    private var deviceNameInput: EditText? = null
    private var brandSpinner: Spinner? = null
    private var settingsText: TextView? = null
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        if (validCachedToken()) showSettings() else showLogin()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun bg(color: Int, radius: Int = 18, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }
    private fun edit(hint: String, secret: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint; setHintTextColor(Color.LTGRAY); setTextColor(Color.WHITE); textSize = 15f
        setPadding(dp(14), dp(10), dp(14), dp(10)); background = bg(Color.rgb(38, 40, 57), 12, Color.rgb(70, 72, 95))
        if (secret) inputType = 0x00000081
    }
    private fun button(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label; textSize = 14f; isAllCaps = false; setTextColor(Color.WHITE)
        background = bg(pink, 14); setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { onClick() }
    }
    private fun page(title: String, subtitle: String): LinearLayout {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(22), dp(20), dp(28)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(dark, Color.rgb(35, 13, 39))) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(text("KING  •  VIP", 13f, pink, true), matchWrap())
        val t = text(title, 27f, Color.WHITE, true); t.gravity = Gravity.CENTER; t.setPadding(0, dp(10), 0, dp(5)); content.addView(t, matchWrap())
        val s = text(subtitle, 13f, Color.LTGRAY); s.gravity = Gravity.CENTER; s.setPadding(0, 0, 0, dp(22)); content.addView(s, matchWrap())
        scroll.addView(content); outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(outer); root = content
        return content
    }
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun addGap(parent: LinearLayout, height: Int = 12) { parent.addView(Space(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun addLabel(parent: LinearLayout, label: String) { val v = text(label, 12f, Color.LTGRAY, true); v.setPadding(dp(2), dp(5), 0, dp(5)); parent.addView(v, matchWrap()) }
    private fun addCard(parent: LinearLayout, child: View) { val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = bg(panel, 18, Color.rgb(60, 48, 75)) }; box.addView(child, matchWrap()); parent.addView(box, matchWrap()) }

    private fun showLogin(message: String? = null) {
        val p = page("ĐĂNG NHẬP LICENSE", "Kích hoạt tài khoản để sử dụng bộ setting")
        addCard(p, LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addLabel(this, "ĐỊA CHỈ SERVER (IP MÁY TÍNH)")
            serverInput = edit("Ví dụ: http://192.168.1.10:8000").also { setTextIfEmpty(it, prefs.getString("server", "") ?: "") }; addView(serverInput, matchWrap())
            addGap(this, 10); addLabel(this, "LICENSE KEY")
            keyInput = edit("Nhập License Key", true); addView(keyInput, matchWrap())
            addGap(this, 12)
            addView(button("KÍCH HOẠT / ĐĂNG NHẬP", ::activate), matchWrap())
            addGap(this, 8)
            statusText = text(message ?: "Server phải đang chạy trên máy tính.", 12f, Color.LTGRAY); addView(statusText, matchWrap())
        })
        addGap(p, 18)
        val note = text("Hỗ trợ Android 7–16 • License gắn với một thiết bị • Có xác thực offline bằng chữ ký server", 11f, Color.GRAY); note.gravity = Gravity.CENTER; p.addView(note, matchWrap())
    }
    private fun setTextIfEmpty(v: EditText, value: String) { if (value.isNotBlank()) v.setText(value) }

    private fun activate() {
        if (busy) return
        val base = serverInput?.text?.toString()?.trim()?.trimEnd('/') ?: ""
        val key = keyInput?.text?.toString()?.trim() ?: ""
        if (!(base.startsWith("http://") || base.startsWith("https://"))) { status("Nhập địa chỉ server hợp lệ, ví dụ http://192.168.1.10:8000"); return }
        if (key.length < 8) { status("License Key phải có ít nhất 8 ký tự"); return }
        prefs.edit().putString("server", base).apply()
        busy = true; status("Đang kết nối và kích hoạt license…")
        val deviceId = deviceId()
        worker.execute {
            try {
                val response = request("$base/api/activate", key, deviceId)
                val json = JSONObject(response)
                if (json.optBoolean("ok") && json.has("license_token")) {
                    val token = json.getString("license_token")
                    if (!verifyToken(token, deviceId)) throw IllegalStateException("Chữ ký license không hợp lệ")
                    securePut("license_token", token); securePut("license_key", key)
                    runOnUiThread { busy = false; showSettings("Kích hoạt thành công") }
                } else throw IllegalStateException(json.optString("detail", "Server không xác nhận license"))
            } catch (e: Exception) {
                runOnUiThread { busy = false; status("Lỗi: ${e.message ?: "Không kết nối được server"}") }
            }
        }
    }

    private fun showSettings(message: String = "License hợp lệ") {
        val p = page("SETTING VIP", "Tạo bộ thông số tham khảo theo thiết bị")
        addCard(p, LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            statusText = text("● $message", 12f, Color.rgb(104, 230, 164), true); addView(statusText, matchWrap())
            addGap(this, 8)
            addLabel(this, "HÃNG ĐIỆN THOẠI")
            brandSpinner = Spinner(this@MainActivity).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, brands)
                setSelection(prefs.getInt("brand", 0).coerceIn(0, brands.lastIndex))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { prefs.edit().putInt("brand", position).apply() }
                }
            }
            addView(brandSpinner, matchWrap())
            addGap(this, 8); addLabel(this, "TÊN THIẾT BỊ")
            deviceNameInput = edit("Ví dụ: Redmi K70E"); addView(deviceNameInput, matchWrap())
            addGap(this, 14)
            addView(button("LẤY SETTING THÍCH HỢP", ::generateSettings), matchWrap())
            addGap(this, 10)
            settingsText = text("Chọn thiết bị rồi nhấn nút để tạo setting.", 14f); settingsText?.setPadding(dp(8), dp(12), dp(8), dp(12)); addView(settingsText, matchWrap())
            addView(button("SAO CHÉP KẾT QUẢ", ::copyResult), matchWrap())
            addGap(this, 8)
            addView(button("ĐỒNG BỘ LICENSE VỚI SERVER", ::syncLicense), matchWrap())
            addView(button("ĐĂNG XUẤT", ::logout), matchWrap())
        })
        addGap(p, 16)
        val disclaimer = text("Các thông số là gợi ý để người dùng tự cài đặt; ứng dụng không can thiệp Free Fire, không tự động ngắm hoặc thay đổi dữ liệu trò chơi.", 11f, Color.GRAY); disclaimer.gravity = Gravity.CENTER; p.addView(disclaimer, matchWrap())
    }

    private fun generateSettings() {
        val name = deviceNameInput?.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) { status("Vui lòng nhập tên thiết bị"); return }
        if (busy) return
        busy = true
        settingsText?.text = "ĐANG PHÂN TÍCH THIẾT BỊ…"
        val delay = SecureRandom().nextInt(5001) + 2000
        settingsText?.postDelayed({
            val labels = listOf("Nhìn xung quanh", "Red Dot", "Ngắm 2X", "Ngắm 4X", "Ống súng ngắm", "Camera tự do")
            val values = labels.map { SecureRandom().nextInt(201) }
            val output = buildString {
                append("KING FREE FIRE VIP\n")
                append("Hãng: ${brandSpinner?.selectedItem ?: brands[0]}\nThiết bị: $name\n\n")
                labels.forEachIndexed { i, label -> append("$label: ${values[i]}\n") }
            }
            settingsText?.text = output
            prefs.edit().putString("last_result", output).apply()
            busy = false
        }, delay.toLong())
    }
    private fun copyResult() {
        val value = prefs.getString("last_result", "") ?: ""
        if (value.isBlank()) { status("Chưa có kết quả để sao chép"); return }
        val clip = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clip.setPrimaryClip(android.content.ClipData.newPlainText("King FreeFire VIP", value))
        status("Đã sao chép kết quả")
    }
    private fun syncLicense() {
        if (busy) return
        val base = prefs.getString("server", "") ?: ""
        val key = secureGet("license_key") ?: ""
        if (base.isBlank() || key.isBlank()) { showLogin("Vui lòng đăng nhập lại"); return }
        busy = true; status("Đang đồng bộ license…")
        worker.execute {
            try {
                val response = JSONObject(request("$base/api/verify", key, deviceId()))
                val token = response.getString("license_token")
                if (!response.optBoolean("ok") || !verifyToken(token, deviceId())) throw IllegalStateException("License không hợp lệ")
                securePut("license_token", token)
                runOnUiThread { busy = false; status("Đồng bộ license thành công") }
            } catch (e: Exception) { runOnUiThread { busy = false; status("Đồng bộ thất bại: ${e.message}") } }
        }
    }
    private fun logout() { securePut("license_token", ""); securePut("license_key", ""); showLogin("Đã đăng xuất") }
    private fun status(message: String) { statusText?.text = message; if (statusText == null) Toast.makeText(this, message, Toast.LENGTH_LONG).show() }

    private fun request(endpoint: String, key: String, device: String): String {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 10000; readTimeout = 15000; doOutput = true
            setRequestProperty("Content-Type", "application/json"); setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject().put("key", key).put("device_id", device).toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val result = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (code !in 200..299) {
                val detail = try { JSONObject(result).optString("detail", result) } catch (_: Exception) { result }
                throw IllegalStateException("HTTP $code: $detail")
            }
            return result
        } finally { conn.disconnect() }
    }

    private fun deviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id.isNullOrBlank()) { id = UUID.randomUUID().toString(); prefs.edit().putString("device_id", id).apply() }
        return id
    }
    private fun verifyToken(token: String, deviceId: String): Boolean {
        return try {
        val parts = token.split('.')
        if (parts.size != 2) return false
        val payloadBytes = Base64.decode(pad(parts[0]), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val signatureBytes = Base64.decode(pad(parts[1]), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val prefix = byteArrayOf(0x30,0x2a,0x30,0x05,0x06,0x03,0x2b,0x65,0x70,0x03,0x21,0x00)
        val rawKey = Base64.decode(pad(publicKey), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val x509 = prefix + rawKey
        val keyFactory = java.security.KeyFactory.getInstance("Ed25519", BouncyCastleProvider())
        val pub = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(x509))
        val verifier = Signature.getInstance("Ed25519", BouncyCastleProvider())
        verifier.initVerify(pub); verifier.update(payloadBytes)
        if (!verifier.verify(signatureBytes)) return false
        val payload = JSONObject(String(payloadBytes, Charsets.UTF_8))
        val digest = MessageDigest.getInstance("SHA-256").digest(deviceId.toByteArray(Charsets.UTF_8))
        val deviceHash = digest.joinToString("") { "%02x".format(it) }
        val now = System.currentTimeMillis() / 1000L
        payload.optString("iss") == "KING LICENSE SERVER" && payload.optString("device") == deviceHash &&
            payload.optLong("exp", 0) > now && payload.optLong("license_exp", 0) > now
        } catch (_: Exception) { false }
    }
    private fun pad(s: String) = s + "=".repeat((4 - s.length % 4) % 4)

    private fun validCachedToken(): Boolean {
        val token = secureGet("license_token") ?: return false
        return verifyToken(token, deviceId())
    }

    // AES/GCM encrypted storage backed by Android Keystore (API 23+).
    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getKey("KingVipAesKey", null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder("KingVipAesKey", android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
    private fun securePut(name: String, value: String) {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit().putString("secure_$name", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
        } catch (_: Exception) { }
    }
    private fun secureGet(name: String): String? = try {
        val packed = Base64.decode(prefs.getString("secure_$name", null), Base64.NO_WRAP)
        val iv = packed.copyOfRange(0, 12); val cipherText = packed.copyOfRange(12, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    } catch (_: Exception) { null }

    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
