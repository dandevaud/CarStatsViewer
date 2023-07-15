package com.ixam97.carStatsViewer.liveDataApi.abrpLiveData

data class AbrpDataSet(
    val stateOfCharge: Int,
    val power: Float,
    val isCharging: Boolean,
    val speed: Float,
    val isParked: Boolean,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?,
    val temp: Float
)
