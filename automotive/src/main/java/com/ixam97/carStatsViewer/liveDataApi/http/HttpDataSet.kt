package com.ixam97.carStatsViewer.liveDataApi.http

data class HttpDataSet(
    val timestamp: Long?,
    val currentSpeed: Float?,
    val currentPower: Float?,
    val currentGear: Int?,
    val chargePortConnected: Boolean?,
    val batteryLevel: Float?,
    val stateOfCharge: Int?,
    val currentIgnitionState: Int?,
    val instConsumption: Float?,
    val driveState: Int?,
    val ambientTemperature: Float?,
    val maxBatteryLevel: Float?,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?,

    // Helpers
    val isCharging: Boolean?,
    val isParked: Boolean?,
    val isFastCharging: Boolean?,

    // ABRP debug
    val abrpPackage: String? = null,

    var driving_session_id: Long?,
    val start_epoch_time: Long?,
    val end_epoch_time: Long?,
    val session_type: Int?,
    val drive_time: Long?,
    val used_energy: Double?,
    val used_soc: Double?,
    val used_soc_energy: Double?,
    val driven_distance: Double?,
    val note: String?,
    val last_edited_epoch_time: Long?,

    // Delta Values
    val powerUsed: Float?,
    val traveledDistance: Float?,
    val timeSpan: Long? ,
)
