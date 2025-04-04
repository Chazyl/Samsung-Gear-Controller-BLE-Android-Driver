package com.example.samsunggearcontrollerbleandroiddriver.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import com.example.samsunggearcontrollerbleandroiddriver.R
import kotlin.math.max
import kotlin.math.min

/**
 * Manager class for handling the mouse pointer overlay.
 */
class PointerOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "PointerOverlayManager"

        // Sensitivity factor for pointer movement
        private const val SENSITIVITY = 50.0f

        // Maximum pointer movement per update
        private const val MAX_MOVEMENT = 20.0f
    }

    // Window manager for overlay
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Main thread handler
    private val mainHandler = Handler(Looper.getMainLooper())

    // Pointer view
    private var pointerView: ImageView? = null

    // Layout parameters for the pointer
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = 0
        y = 0
    }

    // Current pointer position
    private var pointerX = 0
    private var pointerY = 0

    // Screen dimensions
    private var screenWidth = 0
    private var screenHeight = 0

    // Pointer visibility state
    private var isPointerVisible = false

    init {
        // Get screen dimensions
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // Initialize pointer position to center of screen
        pointerX = screenWidth / 2
        pointerY = screenHeight / 2
    }

    /**
     * Shows the pointer overlay.
     */
    fun showPointer() {
        if (isPointerVisible) return

        // Run on main thread
        mainHandler.post {
            try {
                // Inflate the pointer view
                val inflater = LayoutInflater.from(context)
                pointerView = inflater.inflate(R.layout.pointer_overlay, null) as ImageView

                // Set initial position
                layoutParams.x = pointerX
                layoutParams.y = pointerY

                // Add the view to the window manager
                windowManager.addView(pointerView, layoutParams)

                isPointerVisible = true

                Log.d(TAG, "Pointer overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing pointer overlay", e)
            }
        }
    }

    /**
     * Hides the pointer overlay.
     */
    fun hidePointer() {
        if (!isPointerVisible) return

        // Run on main thread
        mainHandler.post {
            try {
                // Remove the view from the window manager
                pointerView?.let {
                    windowManager.removeView(it)
                    pointerView = null
                }

                isPointerVisible = false

                Log.d(TAG, "Pointer overlay hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding pointer overlay", e)
            }
        }
    }

    /**
     * Updates the pointer position based on gyroscope data.
     */
    fun updatePointerPosition(gyroX: Float, gyroY: Float) {
        if (!isPointerVisible) return

        // Calculate movement delta
        val deltaX = (gyroX * SENSITIVITY).coerceIn(-MAX_MOVEMENT, MAX_MOVEMENT).toInt()
        val deltaY = (gyroY * SENSITIVITY).coerceIn(-MAX_MOVEMENT, MAX_MOVEMENT).toInt()

        // Update pointer position
        pointerX = min(max(pointerX + deltaX, 0), screenWidth)
        pointerY = min(max(pointerY + deltaY, 0), screenHeight)

        // Update layout parameters
        layoutParams.x = pointerX
        layoutParams.y = pointerY

        // Update the view on the main thread
        mainHandler.post {
            pointerView?.let {
                try {
                    windowManager.updateViewLayout(it, layoutParams)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating pointer position", e)
                }
            }
        }
    }

    /**
     * Gets the current pointer position.
     */
    fun getPointerPosition(): Pair<Int, Int> {
        return Pair(pointerX, pointerY)
    }
}
