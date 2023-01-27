package dev.boessi.carStatsViewer.objects

import dev.boessi.carStatsViewer.plot.PlotLineItem
import dev.boessi.carStatsViewer.plot.PlotLineMarkerType
import dev.boessi.carStatsViewer.plot.PlotMarker
import java.util.*

data class TripData(
    var appVersion: String,
    var saveDate: Date,
    var traveledDistance: Float,
    var usedEnergy: Float,
    var averageConsumption: Float,
    var travelTimeMillis: Long,
    var lastPlotDistance: Float,
    var lastPlotEnergy: Float,
    var lastPlotTime: Long,
    var lastPlotGear: Int,
    var lastPlotMarker: PlotLineMarkerType?,
    var lastChargePower:Float,
    var consumptionPlotLine: List<PlotLineItem>,
    var chargeCurves: List<ChargeCurve>,
    var markers: List<PlotMarker>
) {

}
