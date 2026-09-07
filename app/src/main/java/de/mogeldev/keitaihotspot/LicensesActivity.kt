package de.mogeldev.keitaihotspot

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import de.mogeldev.keitaihotspot.databinding.ActivityLicensesBinding

class LicensesActivity : ComponentActivity() {

    private lateinit var binding: ActivityLicensesBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Nur zur Laufzeit tatsächlich mitgelieferte Bibliotheken (keine reinen Test-Dependencies).
        binding.textThirdPartyList.text = THIRD_PARTY_LIBRARIES.joinToString("\n") {
            "${it.name} ${it.version} — ${it.license}"
        }

        binding.btnViewGpl.setOnClickListener {
            openUrl("https://github.com/mogeldev/keitaihotspot/blob/main/LICENSE")
        }
        binding.btnViewApache.setOnClickListener {
            openUrl("https://www.apache.org/licenses/LICENSE-2.0")
        }
        binding.btnViewFullList.setOnClickListener {
            openUrl("https://github.com/mogeldev/keitaihotspot/blob/main/THIRD_PARTY_LICENSES.md")
        }

        binding.btnViewGpl.requestFocus()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private data class Library(val name: String, val version: String, val license: String)

    companion object {
        private val THIRD_PARTY_LIBRARIES = listOf(
            Library("Kotlin Standard Library", "2.2.10", "Apache License 2.0"),
            Library("AndroidX Core KTX", "1.19.0", "Apache License 2.0"),
            Library("AndroidX Activity KTX", "1.9.3", "Apache License 2.0"),
            Library("AndroidX Lifecycle Runtime KTX", "2.6.1", "Apache License 2.0"),
            Library("ZXing Core", "3.5.3", "Apache License 2.0"),
        )
    }
}
