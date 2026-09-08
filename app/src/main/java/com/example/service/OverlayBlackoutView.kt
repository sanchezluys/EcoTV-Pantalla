package com.example.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Fullscreen black overlay view for Google TV.
 * Receives all D-pad and remote control key events and touch events.
 * Immediately invokes `onDismissRequest` on ANY button press or interaction.
 */
@SuppressLint("ViewConstructor")
class OverlayBlackoutView(
    context: Context,
    private val onDismissRequest: () -> Unit
) : FrameLayout(context) {

    private val hintContainer: LinearLayout

    init {
        // True black background for OLED 0-watt power consumption
        setBackgroundColor(Color.BLACK)

        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true

        // Center hint container
        hintContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)

            val titleView = TextView(context).apply {
                text = "EcoTV Pantalla"
                setTextColor(Color.argb(160, 46, 213, 115)) // Subtle soft eco green
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            }

            val subtitleView = TextView(context).apply {
                text = "Pantalla apagada • Presiona cualquier botón del control remoto para encender"
                setTextColor(Color.argb(140, 255, 255, 255))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                gravity = Gravity.CENTER
            }

            addView(titleView)
            addView(subtitleView)
        }

        val layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        addView(hintContainer, layoutParams)

        // Automatically fade out the hint to pure 100% black after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAttachedToWindow) {
                ObjectAnimator.ofFloat(hintContainer, View.ALPHA, 1f, 0f).apply {
                    duration = 800
                    start()
                }
            }
        }, 2500)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        requestFocus()
    }

    /**
     * Intercepts ANY remote control key press (D-pad, OK, Back, Numbers, Volume, etc.).
     * Immediately dismisses the overlay and restores original brightness.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            onDismissRequest()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Intercepts screen touches or clicks (e.g. on touchscreen TVs, tablets or mouse in emulator).
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            onDismissRequest()
            return true
        }
        return true
    }
}
