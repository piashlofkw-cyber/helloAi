package com.myname.jarvisai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "JarvisAccessibility"
        private var instance: JarvisAccessibilityService? = null

        fun getInstance(): JarvisAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Jarvis Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Monitor accessibility events for context awareness
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Window changed: ${event.packageName}")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "View clicked: ${event.text}")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Jarvis Accessibility Service interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // Device Automation Functions

    fun openApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Log.i(TAG, "Opened app: $packageName")
                true
            } else {
                Log.w(TAG, "App not found: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $packageName", e)
            false
        }
    }

    fun goBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.i(TAG, "Performed back action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing back action", e)
            false
        }
    }

    fun goHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            Log.i(TAG, "Performed home action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing home action", e)
            false
        }
    }

    fun openNotifications(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            Log.i(TAG, "Opened notifications")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notifications", e)
            false
        }
    }

    fun openQuickSettings(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            Log.i(TAG, "Opened quick settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening quick settings", e)
            false
        }
    }

    fun clickAtPosition(x: Float, y: Float): Boolean {
        return try {
            val path = Path()
            path.moveTo(x, y)

            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))

            dispatchGesture(gestureBuilder.build(), null, null)
            Log.i(TAG, "Clicked at position: ($x, $y)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking at position", e)
            false
        }
    }

    fun findAndClickButton(buttonText: String): Boolean {
        return try {
            val rootNode = rootInActiveWindow ?: return false
            val button = findNodeByText(rootNode, buttonText)

            if (button != null && button.isClickable) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.i(TAG, "Clicked button: $buttonText")
                true
            } else {
                Log.w(TAG, "Button not found or not clickable: $buttonText")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking button", e)
            false
        }
    }

    private fun findNodeByText(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) return result
        }

        return null
    }
}
