package com.myname.jarvisai.services

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class JarvisCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "JarvisCallScreening"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val phoneNumber = callDetails.handle?.schemeSpecificPart ?: "Unknown"
            Log.d(TAG, "Screening call from: $phoneNumber")

            // Get caller information
            val callerDisplayName = callDetails.callerDisplayName
            val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING

            if (!isIncoming) {
                // Don't screen outgoing calls
                respondToCall(callDetails, buildAllowResponse())
                return
            }

            // AI-based spam detection logic
            val isSpam = detectSpam(phoneNumber, callerDisplayName)
            val isScam = detectScam(phoneNumber)

            val response = if (isSpam || isScam) {
                Log.w(TAG, "Potential spam/scam detected: $phoneNumber")
                buildRejectResponse(
                    disallowCall = true,
                    rejectCall = true,
                    skipNotification = true,
                    skipCallLog = false
                )
            } else {
                Log.i(TAG, "Call allowed: $phoneNumber")
                buildAllowResponse()
            }

            respondToCall(callDetails, response)

        } catch (e: Exception) {
            Log.e(TAG, "Error screening call", e)
            // Allow call on error to avoid blocking legitimate calls
            respondToCall(callDetails, buildAllowResponse())
        }
    }

    private fun buildAllowResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private fun buildRejectResponse(
        disallowCall: Boolean,
        rejectCall: Boolean,
        skipNotification: Boolean,
        skipCallLog: Boolean
    ): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(disallowCall)
            .setRejectCall(rejectCall)
            .setSkipCallLog(skipCallLog)
            .setSkipNotification(skipNotification)
            .build()
    }

    private fun detectSpam(phoneNumber: String, callerName: String?): Boolean {
        // Basic spam detection heuristics
        // In production, integrate with a spam database API
        
        // Check for known spam patterns
        val spamPatterns = listOf(
            "^1{10}$",  // All 1s
            "^0{10}$",  // All 0s
            "^(\\d)\\1{9}$"  // Repeating digits
        )
        
        return spamPatterns.any { pattern ->
            phoneNumber.matches(Regex(pattern))
        }
    }

    private fun detectScam(phoneNumber: String): Boolean {
        // Check for international scam patterns
        // Common scam prefixes
        val scamPrefixes = listOf(
            "+234",  // Nigeria
            "+91",   // India (common for scam calls)
            "+92"    // Pakistan
        )
        
        return scamPrefixes.any { prefix ->
            phoneNumber.startsWith(prefix)
        }
    }
}
