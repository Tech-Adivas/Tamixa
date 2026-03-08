package com.araro.application.port

/**
 * Device fingerprint tracking for fraud prevention.
 * Financial safety: limit free trial to one device per account.
 */
interface DevicePort {
    fun registerDevice(parentId: Long, deviceHash: String)
    fun hasDevice(parentId: Long, deviceHash: String): Boolean
    fun getDeviceCount(parentId: Long): Long
}
