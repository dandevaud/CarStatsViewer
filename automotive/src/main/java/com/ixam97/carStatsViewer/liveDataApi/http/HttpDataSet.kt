package com.ixam97.carStatsViewer.liveDataApi.http

data class HttpDataSet(
    val apiVersion: Int = 2,
    val appVersion: String,
    val timestamp: Long,
    val speed: Float,
    val power: Float,
    val selectedGear: String,
    val ignitionState: String,
    val chargePortConnected: Boolean,
    val batteryLevel: Float,
    val stateOfCharge: Float,
    val ambientTemperature: Float,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?,

    // ABRP debug
    val abrpPackage: String? = null,

    var driving_session_id: Long?,
    val start_epoch_time: Long?,
    val end_epoch_time: Long?,
    val session_type: Int?,
    val drive_time: Long?,
    val trip_time: Long?,
    val used_energy: Double?,
    val used_soc: Double?,
    val used_soc_energy: Double?,
    val driven_distance: Double?,
    val note: String?,
    val last_edited_epoch_time: Long?,

    // Delta Values
    val powerUsed: Float?,
    val traveledDistance: Float?,
    val timeSpan: Long?
)
