package com.example.samsunggearcontrollerbleandroiddriver.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.media.AudioManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.samsunggearcontrollerbleandroiddriver.model.TouchpadDirection

/**
 * Accessibility service for simulating inputs from the Samsung Gear Controller.
 */
class GearControllerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GearControllerAccessibilityService"

        // Intent actions
        const val ACTION_UPDATE_CONTROLLER_STATE = "com.example.samsunggearcontrollerbleandroiddriver.ACTION_UPDATE_CONTROLLER_STATE"

        // Intent extras
        const val EXTRA_VOLUME_UP = "extra_volume_up"
        const val EXTRA_VOLUME_DOWN = "extra_volume_down"
        const val EXTRA_HOME = "extra_home"
        const val EXTRA_BACK = "extra_back"
        const val EXTRA_TRIGGER = "extra_trigger"
        const val EXTRA_TOUCHPAD_CLICK = "extra_touchpad_click"
        const val EXTRA_TOUCHPAD_X = "extra_touchpad_x"
        const val EXTRA_TOUCHPAD_Y = "extra_touchpad_y"

        // Debounce time for button actions (milliseconds)
        private const val DEBOUNCE_TIME = 300L
    }

    // Audio manager for volume control
    private lateinit var audioManager: AudioManager

    // Broadcast receiver for controller state updates
    private val controllerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_CONTROLLER_STATE) {
                handleControllerStateUpdate(intent)
            }
        }
    }

    // Button state tracking
    private var volumeUpPressed = false
    private var volumeDownPressed = false
    private var homePressed = false
    private var backPressed = false
    private var triggerPressed = false
    private var touchpadClickPressed = false

    // Touchpad state tracking
    private var touchpadX = 0f
    private var touchpadY = 0f
    private var currentTouchpadDirection: TouchpadDirection? = null

    // Last action timestamps for debouncing
    private val lastActionTime = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")

        // Initialize audio manager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Register broadcast receiver
        val filter = IntentFilter(ACTION_UPDATE_CONTROLLER_STATE)
        registerReceiver(controllerStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")

        // Unregister broadcast receiver
        try {
            unregisterReceiver(controllerStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used in this implementation
    }

    override fun onInterrupt() {
        // Not used in this implementation
    }

    /**
     * Handles controller state updates from the broadcast receiver.
     */
    private fun handleControllerStateUpdate(intent: Intent) {
        // Get button states
        val newVolumeUpPressed = intent.getBooleanExtra(EXTRA_VOLUME_UP, false)
        val newVolumeDownPressed = intent.getBooleanExtra(EXTRA_VOLUME_DOWN, false)
        val newHomePressed = intent.getBooleanExtra(EXTRA_HOME, false)
        val newBackPressed = intent.getBooleanExtra(EXTRA_BACK, false)
        val newTriggerPressed = intent.getBooleanExtra(EXTRA_TRIGGER, false)
        val newTouchpadClickPressed = intent.getBooleanExtra(EXTRA_TOUCHPAD_CLICK, false)

        // Get touchpad coordinates
        val newTouchpadX = intent.getFloatExtra(EXTRA_TOUCHPAD_X, 0f)
        val newTouchpadY = intent.getFloatExtra(EXTRA_TOUCHPAD_Y, 0f)

        // Handle button press events
        if (newVolumeUpPressed && !volumeUpPressed) {
            handleVolumeUp()
        }

        if (newVolumeDownPressed && !volumeDownPressed) {
            handleVolumeDown()
        }

        if (newHomePressed && !homePressed) {
            handleHome()
        }

        if (newBackPressed && !backPressed) {
            handleBack()
        }

        if (newTriggerPressed && !triggerPressed) {
            handleTrigger()
        }

        if (newTouchpadClickPressed && !touchpadClickPressed) {
            handleTouchpadClick()
        }

        // Handle touchpad direction changes
        if (newTouchpadX != touchpadX || newTouchpadY != touchpadY) {
            handleTouchpadMovement(newTouchpadX, newTouchpadY)
        }

        // Update button states
        volumeUpPressed = newVolumeUpPressed
        volumeDownPressed = newVolumeDownPressed
        homePressed = newHomePressed
        backPressed = newBackPressed
        triggerPressed = newTriggerPressed
        touchpadClickPressed = newTouchpadClickPressed

        // Update touchpad coordinates
        touchpadX = newTouchpadX
        touchpadY = newTouchpadY
    }

    /**
     * Handles volume up button press.
     */
    private fun handleVolumeUp() {
        if (!shouldPerformAction("volume_up")) return

        Log.d(TAG, "Volume Up pressed")
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Handles volume down button press.
     */
    private fun handleVolumeDown() {
        if (!shouldPerformAction("volume_down")) return

        Log.d(TAG, "Volume Down pressed")
        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Handles home button press.
     */
    private fun handleHome() {
        if (!shouldPerformAction("home")) return

        Log.d(TAG, "Home pressed")
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Handles back button press.
     */
    private fun handleBack() {
        if (!shouldPerformAction("back")) return

        Log.d(TAG, "Back pressed")
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Handles trigger button press.
     */
    private fun handleTrigger() {
        if (!shouldPerformAction("trigger")) return

        Log.d(TAG, "Trigger pressed")

        // Get pointer position from service
        val pointerPosition = getPointerPosition()

        // Simulate a tap at the pointer position
        val path = Path()
        path.moveTo(pointerPosition.first.toFloat(), pointerPosition.second.toFloat())

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Handles touchpad click.
     */
    private fun handleTouchpadClick() {
        if (!shouldPerformAction("touchpad_click")) return

        Log.d(TAG, "Touchpad clicked")
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Handles touchpad movement.
     */
    private fun handleTouchpadMovement(x: Float, y: Float) {
        // Determine touchpad direction
        val direction = getTouchpadDirection(x, y)

        // Only process if direction changed
        if (direction != currentTouchpadDirection) {
            currentTouchpadDirection = direction

            // Handle direction change
            when (direction) {
                TouchpadDirection.UP -> handleTouchpadUp()
                TouchpadDirection.DOWN -> handleTouchpadDown()
                TouchpadDirection.LEFT -> handleTouchpadLeft()
                TouchpadDirection.RIGHT -> handleTouchpadRight()
                null -> {
                    // No direction active
                }
            }
        }
    }

    /**
     * Handles touchpad up direction.
     */
    private fun handleTouchpadUp() {
        if (!shouldPerformAction("touchpad_up")) return

        Log.d(TAG, "Touchpad Up")
        // Use a gesture to simulate scrolling up
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        path.moveTo(width / 2f, height * 0.7f)
        path.lineTo(width / 2f, height * 0.3f)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Handles touchpad down direction.
     */
    private fun handleTouchpadDown() {
        if (!shouldPerformAction("touchpad_down")) return

        Log.d(TAG, "Touchpad Down")
        // Use a gesture to simulate scrolling down
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        path.moveTo(width / 2f, height * 0.3f)
        path.lineTo(width / 2f, height * 0.7f)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Handles touchpad left direction.
     */
    private fun handleTouchpadLeft() {
        if (!shouldPerformAction("touchpad_left")) return

        Log.d(TAG, "Touchpad Left")
        // Simulate a swipe from right to left
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        path.moveTo(width * 0.7f, height / 2f)
        path.lineTo(width * 0.3f, height / 2f)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Handles touchpad right direction.
     */
    private fun handleTouchpadRight() {
        if (!shouldPerformAction("touchpad_right")) return

        Log.d(TAG, "Touchpad Right")
        // Simulate a swipe from left to right
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val path = Path()
        path.moveTo(width * 0.3f, height / 2f)
        path.lineTo(width * 0.7f, height / 2f)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Determines the touchpad direction based on coordinates.
     */
    private fun getTouchpadDirection(x: Float, y: Float): TouchpadDirection? {
        if (Math.abs(x) < 0.5f && Math.abs(y) < 0.5f) return null

        return when {
            y < -0.5f -> TouchpadDirection.UP
            y > 0.5f -> TouchpadDirection.DOWN
            x < -0.5f -> TouchpadDirection.LEFT
            x > 0.5f -> TouchpadDirection.RIGHT
            else -> null
        }
    }

    /**
     * Gets the current pointer position from the service.
     */
    private fun getPointerPosition(): Pair<Int, Int> {
        // Get the pointer position from the service
        // For now, return the center of the screen as a fallback
        val displayMetrics = resources.displayMetrics
        return Pair(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2)
    }

    /**
     * Checks if an action should be performed based on debounce time.
     */
    private fun shouldPerformAction(action: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastActionTime[action] ?: 0L

        return if (currentTime - lastTime > DEBOUNCE_TIME) {
            lastActionTime[action] = currentTime
            true
        } else {
            false
        }
    }
}
