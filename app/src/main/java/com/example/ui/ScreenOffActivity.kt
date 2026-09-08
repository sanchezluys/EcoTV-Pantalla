package com.example.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.manager.BrightnessManager
import com.example.manager.EcoState

/**
 * Fullscreen blackout Activity for EcoTV Pantalla.
 * Handles instant blackout, sets window screen brightness to 0.0f,
 * and dismisses on ANY remote control button press or touch.
 */
class ScreenOffActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ScreenOffActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    private var hintLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure full screen black window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val windowParams = window.attributes
        windowParams.screenBrightness = 0.0f // Set display brightness to absolute minimum
        window.attributes = windowParams

        hideSystemBars()

        // 1. Lower system brightness and save original in memory
        BrightnessManager.saveAndSetMinimumBrightness(this)
        EcoState.setScreenOffActive(true)

        // Create pure pitch black root layout
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
        }

        // Brief dim hint for distance viewing reassurance
        hintLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)

            val title = TextView(this@ScreenOffActivity).apply {
                text = "EcoTV Pantalla"
                setTextColor(Color.argb(160, 46, 213, 115))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }

            val subtitle = TextView(this@ScreenOffActivity).apply {
                text = "Pantalla apagada • Presiona cualquier botón del control remoto para encender"
                setTextColor(Color.argb(140, 255, 255, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                gravity = Gravity.CENTER
            }

            addView(title)
            addView(subtitle)
        }

        root.addView(
            hintLayout,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        setContentView(root)

        // Fade hint to pitch black after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            hintLayout?.let {
                ObjectAnimator.ofFloat(it, View.ALPHA, 1f, 0f).apply {
                    duration = 600
                    start()
                }
            }
        }, 2000)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    /**
     * Reactivates the TV screen on ANY remote control button press (D-pad, OK, Back, etc.).
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        reactivateAndFinish()
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            reactivateAndFinish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            reactivateAndFinish()
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun reactivateAndFinish() {
        BrightnessManager.restoreOriginalBrightness(this)
        EcoState.setScreenOffActive(false)
        finish()
    }

    override fun onDestroy() {
        BrightnessManager.restoreOriginalBrightness(this)
        EcoState.setScreenOffActive(false)
        super.onDestroy()
    }
}
