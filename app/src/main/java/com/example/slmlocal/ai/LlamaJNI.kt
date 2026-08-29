package com.example.slmlocal.ai

import android.util.Log

object LlamaJNI {
    private const val TAG = "LlamaJNI"
    var isLoaded = false
        private set

    init {
        try {
            System.loadLibrary("slmlocal_jni")
            isLoaded = true
            Log.d(TAG, "Successfully loaded slmlocal_jni")
            val sysInfo = systemInfo()
            Log.d(TAG, "llama.cpp System Info: $sysInfo")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load slmlocal_jni. Native build required.", e)
            isLoaded = false
        }
    }

    // Native function stubs
    external fun systemInfo(): String
    external fun loadModel(modelPath: String, contextSize: Int): Long
    
    interface TokenCallback {
        fun onToken(token: String)
    }
    
    external fun generateTokens(contextPtr: Long, prompt: String, callback: TokenCallback)
    external fun cancelGeneration()
    external fun freeModel(contextPtr: Long)
}

