package com.example.samsunggearcontrollerbleandroiddriver.model

/**
 * Data class representing the current state of the Samsung Gear Controller.
 */
data class ControllerState(
    val volumeUp: Boolean = false,
    val volumeDown: Boolean = false,
    val home: Boolean = false,
    val back: Boolean = false,
    val trigger: Boolean = false,
    val touchpadClick: Boolean = false,
    val touchpadX: Float = 0f,
    val touchpadY: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val isMouseMoving: Boolean = false
) {
    /**
     * Returns the active touchpad direction based on the touchpad coordinates.
     * Returns null if no direction is active.
     */
    fun getTouchpadDirection(): TouchpadDirection? {
        if (touchpadX == 0f && touchpadY == 0f) return null
        
        return when {
            touchpadY < -0.5f -> TouchpadDirection.UP
            touchpadY > 0.5f -> TouchpadDirection.DOWN
            touchpadX < -0.5f -> TouchpadDirection.LEFT
            touchpadX > 0.5f -> TouchpadDirection.RIGHT
            else -> null
        }
    }
}

/**
 * Enum representing the possible touchpad directions.
 */
enum class TouchpadDirection {
    UP, DOWN, LEFT, RIGHT
}
