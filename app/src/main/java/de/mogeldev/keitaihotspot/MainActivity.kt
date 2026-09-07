package de.mogeldev.keitaihotspot

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import de.mogeldev.keitaihotspot.databinding.ActivityMainBinding
import java.security.SecureRandom

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private var appliedLanguage: String = ""
    private var currentPassword: String = ""
    private var rootHotspotRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appliedLanguage = LocaleHelper.getSelectedLanguage(this)

        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.btnOpenSystemHotspot.setOnClickListener { openSystemHotspotSettings() }

        currentPassword = generateRandomPassword()
        binding.textRootPassword.text = currentPassword
        binding.btnRootGeneratePassword.setOnClickListener { onGeneratePasswordClicked() }
        binding.btnRootStart.setOnClickListener { onRootStartClicked() }
        binding.btnRootStop.setOnClickListener { onRootStopClicked() }

        binding.btnSettings.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        // Sprache kann im Settings-Screen geändert worden sein -> UI neu aufbauen, falls nötig.
        if (LocaleHelper.getSelectedLanguage(this) != appliedLanguage) {
            recreate()
        }
    }

    /**
     * Öffnet die "echte" System-Hotspot-Seite mit Internet-Freigabe (Abschnitt "Ohne Root").
     * Diese ist seit Android 8 für Drittanbieter-Apps nicht mehr über eine öffentliche
     * Standard-API erreichbar; auf manchen Geräten (u.a. diesem Kyocera-Modell) existiert
     * die Activity aber weiterhin intern, nur ohne Menüeintrag in den Einstellungen.
     * Best-effort: mehrere bekannte Komponentennamen durchprobieren.
     */
    private fun openSystemHotspotSettings() {
        val candidates = listOf(
            ComponentName("com.android.settings", "com.android.settings.TetherSettings"),
            ComponentName("jp.kyocera.settings.nfp", "jp.kyocera.settings.nfp.core.Settings\$TetherSettingsActivity"),
            ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity"),
        )
        for (component in candidates) {
            try {
                startActivity(Intent().apply { this.component = component })
                return
            } catch (e: ActivityNotFoundException) {
                // Nächsten Kandidaten versuchen.
            } catch (e: SecurityException) {
                // Nicht exportiert / kein Zugriff -> nächsten Kandidaten versuchen.
            }
        }
        Toast.makeText(this, getString(R.string.system_hotspot_not_found), Toast.LENGTH_LONG).show()
    }

    private fun onGeneratePasswordClicked() {
        currentPassword = generateRandomPassword()
        binding.textRootPassword.text = currentPassword
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val random = SecureRandom()
        return (1..12).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private fun onRootStartClicked() {
        val ssid = "KeitaiHotspot_" + (1000..9999).random()
        val password = currentPassword
        setRootBusyState(getString(R.string.root_status_starting))

        Thread {
            if (!RootHotspotHelper.isRootAvailable()) {
                mainHandler.post { onRootFailed(getString(R.string.root_not_detected)) }
                return@Thread
            }
            val result = RootHotspotHelper.startHotspot(ssid, password)
            mainHandler.post {
                if (result.success) {
                    onRootStarted(ssid, password)
                } else {
                    onRootFailed(result.output.ifBlank { "unknown error" })
                }
            }
        }.start()
    }

    private fun onRootStopClicked() {
        setRootBusyState(getString(R.string.root_status_stopping))

        Thread {
            val result = RootHotspotHelper.stopHotspot()
            mainHandler.post {
                if (result.success) {
                    onRootStopped()
                } else {
                    onRootFailed(result.output.ifBlank { "unknown error" })
                }
            }
        }.start()
    }

    private fun setRootBusyState(statusText: String) {
        binding.textRootStatus.text = statusText
        binding.btnRootStart.isEnabled = false
        binding.btnRootStop.isEnabled = false
        binding.btnRootGeneratePassword.isEnabled = false
    }

    private fun onRootStarted(ssid: String, password: String) {
        rootHotspotRunning = true
        binding.textRootStatus.text = getString(R.string.root_status_running)
        binding.textRootSsid.text = ssid
        binding.textRootPassword.text = password
        binding.btnRootStart.isEnabled = false
        binding.btnRootStop.isEnabled = true
        binding.btnRootGeneratePassword.isEnabled = false

        val qrContent = "WIFI:T:WPA;S:$ssid;P:$password;;"
        binding.imageRootQrCode.setImageBitmap(generateQrBitmap(qrContent, 600))
        binding.imageRootQrCode.visibility = View.VISIBLE
    }

    private fun onRootStopped() {
        rootHotspotRunning = false
        binding.textRootStatus.text = getString(R.string.root_status_idle)
        binding.textRootSsid.text = getString(R.string.placeholder_dash)
        binding.textRootPassword.text = currentPassword
        binding.btnRootStart.isEnabled = true
        binding.btnRootStop.isEnabled = false
        binding.btnRootGeneratePassword.isEnabled = true
        binding.imageRootQrCode.visibility = View.GONE
    }

    private fun onRootFailed(reason: String) {
        binding.textRootStatus.text = getString(R.string.root_status_error, reason)
        binding.btnRootStart.isEnabled = !rootHotspotRunning
        binding.btnRootStop.isEnabled = rootHotspotRunning
        binding.btnRootGeneratePassword.isEnabled = !rootHotspotRunning
        Toast.makeText(this, getString(R.string.root_status_error, reason), Toast.LENGTH_LONG).show()
    }

    private fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
