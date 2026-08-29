package com.example.slmlocal.ai

sealed class EngineState {
    object Uninitialized : EngineState()
    object Loading : EngineState()
    object Ready : EngineState()
    data class Error(val message: String) : EngineState()
}
