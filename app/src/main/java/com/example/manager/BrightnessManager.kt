package com.example.manager

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Manages TV screen brightness in memory.
 * Stores `originalBrightness` in volatile memory upon activation and restores it upon deactivation.
 */
object BrightnessManager {
    private const val TAG = "BrightnessManager"

    // In-memory temporary state (non-persistent between sessions)
    private var originalBrightness: Int? = null

    /**
     * Checks if the app has permission to write system settings (change brightness).
     */
    fun canModifySystemSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Checks if the app has permission to draw overlays over other applications.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Saves the current TV brightness in memory, then lowers the brightness to the minimum possible (0).
     */
    @Synchronized
    fun saveAndSetMinimumBrightness(context: Context) {
        if (!canModifySystemSettings(context)) {
            Log.w(TAG, "WRITE_SETTINGS permission not granted. System brightness cannot be modified directly.")
            return
        }

        try {
            val contentResolver = context.contentResolver
            val currentBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            // Only save if not already saved in current session
            if (originalBrightness == null) {
                originalBrightness = currentBrightness
                Log.d(TAG, "Saved original TV brightness in memory: $currentBrightness")
            }

            // Set brightness to minimum possible value (0)
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                0
            )
            Log.d(TAG, "TV brightness reduced to minimum (0) for energy saving")
        } catch (e: Exception) {
            Log.e(TAG, "Error while saving and lowering brightness", e)
        }
    }

    /**
     * Restores the TV brightness to the level it had before activation and clears in-memory state.
     */
    @Synchronized
    fun restoreOriginalBrightness(context: Context) {
        val saved = originalBrightness ?: return

        if (!canModifySystemSettings(context)) {
            Log.w(TAG, "WRITE_SETTINGS permission not granted. Cannot restore system brightness.")
            originalBrightness = null
            return
        }

        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                saved
            )
            Log.d(TAG, "Restored original TV brightness: $saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error while restoring original brightness", e)
        } finally {
            originalBrightness = null
        }
    }

    /**
     * Returns true if there is an active saved brightness awaiting restoration.
     */
    fun hasSavedBrightness(): Boolean = originalBrightness != null

    /**
     * Inspect current saved brightness for debugging/UI state display.
     */
    fun getSavedBrightness(): Int? = originalBrightness
}
