package de.mogeldev.keitaihotspot

import android.app.Application
import android.content.Context

class KeitaiHotspotApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
