package de.mogeldev.keitaihotspot

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import de.mogeldev.keitaihotspot.databinding.ActivitySettingsBinding
import de.mogeldev.keitaihotspot.databinding.DialogAboutBinding

class SettingsActivity : ComponentActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLanguage.setOnClickListener { showLanguageDialog() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }
        binding.btnLicenses.setOnClickListener { startActivity(Intent(this, LicensesActivity::class.java)) }

        binding.btnLanguage.requestFocus()
    }

    private fun showLanguageDialog() {
        val codes = LocaleHelper.supportedLanguageCodes
        val labels = codes.map { code ->
            if (code == "system") getString(R.string.language_system) else LocaleHelper.nativeName(code)
        }.toTypedArray()
        val currentCode = LocaleHelper.getSelectedLanguage(this)
        val checkedIndex = codes.indexOf(currentCode).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_choose_language)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selected = codes[which]
                dialog.dismiss()
                if (selected != currentCode) {
                    LocaleHelper.setSelectedLanguage(this, selected)
                    recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAboutDialog() {
        val dialogBinding = DialogAboutBinding.inflate(layoutInflater)
        dialogBinding.btnWebsiteGithub.setOnClickListener { openUrl("https://mogeldev.github.io") }
        dialogBinding.btnWebsiteDe.setOnClickListener { openUrl("https://mogeldev.de") }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
