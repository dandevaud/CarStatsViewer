package com.ixam97.carStatsViewer.dataProcessor

data class DeltaData(
    val powerUsed: Float? = null,
    val traveledDistance: Float? = null,
    val startEpoch: Long? = null,
    val endEpoch: Long? = null,
    val refEpoch: Long? = null
) {
    fun timeSpan() : Long? {
        return when {
            startEpoch == null || endEpoch == null -> null
            else -> endEpoch - startEpoch
        }
    }
}
