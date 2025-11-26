package com.example.dresscode.model

/**
 * Lightweight result wrapper for async UI work.
 * Replace or extend when wiring real backend/network responses.
 */
sealed class AsyncResult<out T> {
    data object Idle : AsyncResult<Nothing>()
    data object Loading : AsyncResult<Nothing>()
    data class Success<T>(val data: T) : AsyncResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : AsyncResult<Nothing>()
}
