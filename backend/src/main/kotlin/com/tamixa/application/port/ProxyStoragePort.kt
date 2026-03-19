package com.tamixa.application.port

/**
 * Port for streaming object bytes from backing storage (e.g. S3).
 * Used by audio and cover proxy controllers to avoid direct S3 dependency in API layer.
 */
interface ProxyStoragePort {

    /** Returns full object bytes, or null if not found / error. */
    fun getObject(key: String): ByteArray?

    /** Returns content length in bytes, or null if not found. */
    fun getContentLength(key: String): Long?

    /** Returns bytes for the given byte range (inclusive). Used for Range requests. */
    fun getObjectRange(key: String, start: Long, end: Long): ByteArray?
}
