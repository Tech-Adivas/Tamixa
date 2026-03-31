package com.tamixa.application.port

import com.tamixa.application.storylibrary.BulkJobState

/**
 * Port for storing bulk story generation job state.
 * Implementations: BulkJobStore (in-memory), RedisBulkJobStore (distributed).
 */
interface BulkJobStorePort {
    
    fun create(requestedTotal: Int, publish: Boolean = false): String
    
    fun get(jobId: String): BulkJobState?
    
    fun tryAcquire(jobId: String): Boolean
    
    fun release(jobId: String)
    
    fun isBulkRunning(): Boolean
    
    fun getRunningJobId(): String?
    
    fun updateProgress(
        jobId: String,
        currentIndex: Int,
        createdCount: Int,
        failedCount: Int,
        created: List<Map<String, Any?>>,
        failed: List<Map<String, Any?>>
    )
    
    fun complete(jobId: String, result: Map<String, Any>)
    
    fun fail(jobId: String, message: String)
    
    fun setRunning(jobId: String)
}
