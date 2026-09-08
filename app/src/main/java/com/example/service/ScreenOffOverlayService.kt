package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.manager.BrightnessManager
import com.example.manager.EcoState

/**
 * Foreground Service that manages the system-wide black overlay window and screen brightness.
 * Works over any running app (such as YouTube, Spotify, Podcast players) on Google TV.
 */
class ScreenOffOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: OverlayBlackoutView? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "ScreenOffOverlayService"
        private const val CHANNEL_ID = "ecotv_screen_off_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_OVERLAY = "com.example.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.example.action.STOP_OVERLAY"

        fun startOverlay(context: Context) {
            val intent = Intent(context, ScreenOffOverlayService::class.java).apply {
                action = ACTION_START_OVERLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, ScreenOffOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EcoTV:ScreenOffWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_OVERLAY -> {
                stopScreenOff()
            }
            ACTION_START_OVERLAY, null -> {
                startScreenOff()
            }
        }
        return START_NOT_STICKY
    }

    private fun startScreenOff() {
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification", e)
        }

        try {
            wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours safety timeout
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }

        // 1. Save current TV brightness and set to minimum
        BrightnessManager.saveAndSetMinimumBrightness(this)

        // 2. Attach full-screen black overlay
        if (overlayView == null && BrightnessManager.canDrawOverlays(this)) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.OPAQUE
                ).apply {
                    screenBrightness = 0.0f // Explicitly instruct window manager to lower brightness to minimum
                    gravity = Gravity.FILL
                }

                overlayView = OverlayBlackoutView(this) {
                    // Any remote key press or touch dismisses the overlay!
                    Log.d(TAG, "Remote control key pressed on overlay -> Dismissing screen off")
                    stopScreenOff()
                }

                windowManager?.addView(overlayView, params)
                overlayView?.requestFocus()
                EcoState.setScreenOffActive(true)
                Log.d(TAG, "System overlay attached successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to attach overlay window", e)
            }
        }
    }

    private fun stopScreenOff() {
        // 1. Restore original TV brightness
        BrightnessManager.restoreOriginalBrightness(this)

        // 2. Remove overlay view from WindowManager
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
                Log.d(TAG, "Overlay removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view", e)
            }
            overlayView = null
        }

        // 3. Release WakeLock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }

        EcoState.setScreenOffActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EcoTV Pantalla Ahorro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación de estado mientras la pantalla está apagada"
                setShowBadge(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenOffOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EcoTV Pantalla • Pantalla apagada")
            .setContentText("Ahorro de energía activo. Presiona cualquier botón del control para encender.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_revert,
                "Encender pantalla",
                pendingStop
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopScreenOff()
        super.onDestroy()
    }
}
