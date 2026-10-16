package com.cobplayer.app

import android.app.Application
import com.google.android.gms.cast.framework.CastContext

class CobApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try { CastContext.getSharedInstance(this) } catch (_: Exception) {}
    }
}
