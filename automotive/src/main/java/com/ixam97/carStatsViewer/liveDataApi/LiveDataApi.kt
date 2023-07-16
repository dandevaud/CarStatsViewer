package com.ixam97.carStatsViewer.liveDataApi

import android.content.Context
import android.os.Handler
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.dataProcessor.DeltaData
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class LiveDataApi(
    val apiIdentifier: String,
    val apiNameStringId: Int,
    var detailedLog: Boolean
    ){

    /**
     * Indicates the current connection status of the API
     *      0: Unused
     *      1: Connected
     *      2: Error
     *      3: Limited connection
     */
    var connectionStatus: ConnectionStatus = ConnectionStatus.UNUSED
    var timeout: Int = 5_000
    var originalInterval: Int = 5_000

    enum class ConnectionStatus(val status: Int) {
        UNUSED(0),
        CONNECTED(1),
        ERROR(2),
        LIMITED(3);

        companion object {
            fun fromInt(status: Int) = values().first { it.status == status }
        }
    }

    /**
     * Dialog to setup API.
     */
    abstract fun showSettingsDialog(context: Context)

    /**
     * creates a runnable to be executed in intervals. Returns null if API does not send data in
     * timed intervals.
     */
    open fun createLiveDataTask(
        // dataManager: DataManager,
        realTimeData: RealTimeData,
        drivingSession: DrivingSession?,
        deltaData: DeltaData?,
        handler: Handler,
        interval: Int
    ): Runnable? {
        timeout = interval
        return object : Runnable {
            override fun run() {
                coroutineSendNow(realTimeData, drivingSession,deltaData)
                handler.postDelayed(this, timeout.toLong())
            }
        }
    }

    /**
     * sendNow, but wrapped in a coroutine to not block main thread.
     */
    fun coroutineSendNow(realTimeData: RealTimeData, drivingSession: DrivingSession?,deltaData: DeltaData? ) {
    //    CoroutineScope(Dispatchers.Default).launch {
            sendNow(realTimeData, drivingSession,deltaData)
            updateWatchdog()
    //    }
    }

    fun requestFlow(serviceScope: CoroutineScope, dataProcessor: DataProcessor, interval: Int): Flow<Unit> {
        timeout = interval
        originalInterval = interval
        return flow {
            var delta : DeltaData? = null
            while (true) {
                val newDelta = dataProcessor.getDrivePointDeltaBetween(delta?.refEpoch ?: System.currentTimeMillis())
                coroutineSendNow(dataProcessor.realTimeData, dataProcessor.selectedSessionDataFlow.value, newDelta)

                when (connectionStatus){
                    ConnectionStatus.LIMITED, ConnectionStatus.CONNECTED -> delta = newDelta
                    else -> { }
                }

                delay(timeout.toLong())
            }
        }
    }

    /**
     * Code to be executed in coroutineSendNow. This function should not be called outside a
     * coroutine to not block main thread.
     */
    protected abstract fun sendNow(realTimeData: RealTimeData, drivingSession: DrivingSession?, deltaData: DeltaData?)

    private fun updateWatchdog() {
        val currentApiStateMap = CarStatsViewer.watchdog.getCurrentWatchdogState().apiState.toMutableMap()
        currentApiStateMap[apiIdentifier] = connectionStatus.status
        CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(
            apiState = currentApiStateMap.toMap()
        ))
    }
}