package com.araro.application.port

/**
 * Stores data export files (e.g. JSON) and returns the object key for signed URL generation.
 * Used for GDPR/DPDP data export. When not configured, export jobs remain pending.
 */
interface DataExportStoragePort {

    /**
     * Upload export data and return the S3/GCS object key.
     * @param parentId Parent ID for path scoping
     * @param jobId Export job ID
     * @param jsonBytes UTF-8 JSON bytes
     * @return Object key (e.g. "exports/123/456/araro-data-export.json")
     */
    fun uploadExport(parentId: Long, jobId: Long, jsonBytes: ByteArray): String
}
