package de.mogeldev.keitaihotspot

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * EXPERIMENTELL: Steuert den echten (internetteilenden) Wi-Fi-Hotspot direkt über
 * Root, per "cmd wifi start-softap/stop-softap" (Syntax aus dem AOSP-Quellcode von
 * WifiShellCommand.java). Diese Shell-Befehle verlangen laut Android-Quellcode
 * ausdrücklich UID 0 (echtes Root) — ein normaler ADB-Shell-Zugriff (UID 2000)
 * reicht dafür NICHT aus.
 *
 * Ungetestet auf echter Hardware, da kein gerootetes Testgerät zur Verfügung steht.
 */
object RootHotspotHelper {

    data class CommandResult(val success: Boolean, val output: String)

    fun isRootAvailable(): Boolean {
        val result = runAsRoot("id")
        return result.success && result.output.contains("uid=0")
    }

    fun startHotspot(ssid: String, password: String): CommandResult {
        return runAsRoot("cmd wifi start-softap $ssid wpa2 $password")
    }

    fun stopHotspot(): CommandResult {
        return runAsRoot("cmd wifi stop-softap")
    }

    private fun runAsRoot(command: String): CommandResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val exitCode = process.waitFor()
            CommandResult(exitCode == 0, output)
        } catch (e: Exception) {
            CommandResult(false, e.message ?: "su not available")
        }
    }
}
