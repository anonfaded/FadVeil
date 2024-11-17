package com.fadedhood.fadveil.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fadedhood.fadveil.MainActivity
import com.fadedhood.fadveil.R
import com.fadedhood.fadveil.overlay.OverlayManager

class OverlayService : Service() {
    private var overlayManager: OverlayManager? = null
    
    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        overlayManager?.destroy()
        overlayManager = null
        super.onDestroy()
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FadVeil Overlay Service"
        private const val CHANNEL_NAME = "FadVeil Overlay"
    }
} 