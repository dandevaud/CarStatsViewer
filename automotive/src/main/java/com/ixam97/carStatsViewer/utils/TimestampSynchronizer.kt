package com.ixam97.carStatsViewer.utils

class TimestampSynchronizer {
    companion object {
        private var nanoTime = System.nanoTime()
        private var milliTime = System.currentTimeMillis()

        fun getSystemTimeFromNanosTimestamp(timestamp: Long): Long {
            val nanosDelta = (timestamp - nanoTime) / 1_000_000
            return milliTime + nanosDelta
        }
    }
}