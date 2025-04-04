package com.example.samsunggearcontrollerbleandroiddriver

import com.example.samsunggearcontrollerbleandroiddriver.model.ControllerState
import com.example.samsunggearcontrollerbleandroiddriver.model.TouchpadDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the ControllerState class.
 */
class ControllerStateTest {
    
    @Test
    fun testTouchpadDirectionUp() {
        val state = ControllerState(touchpadX = 0f, touchpadY = -0.8f)
        assertEquals(TouchpadDirection.UP, state.getTouchpadDirection())
    }
    
    @Test
    fun testTouchpadDirectionDown() {
        val state = ControllerState(touchpadX = 0f, touchpadY = 0.8f)
        assertEquals(TouchpadDirection.DOWN, state.getTouchpadDirection())
    }
    
    @Test
    fun testTouchpadDirectionLeft() {
        val state = ControllerState(touchpadX = -0.8f, touchpadY = 0f)
        assertEquals(TouchpadDirection.LEFT, state.getTouchpadDirection())
    }
    
    @Test
    fun testTouchpadDirectionRight() {
        val state = ControllerState(touchpadX = 0.8f, touchpadY = 0f)
        assertEquals(TouchpadDirection.RIGHT, state.getTouchpadDirection())
    }
    
    @Test
    fun testTouchpadDirectionNone() {
        val state = ControllerState(touchpadX = 0f, touchpadY = 0f)
        assertNull(state.getTouchpadDirection())
    }
    
    @Test
    fun testTouchpadDirectionCenter() {
        val state = ControllerState(touchpadX = 0.2f, touchpadY = 0.2f)
        assertNull(state.getTouchpadDirection())
    }
}
