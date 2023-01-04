package com.ixam97.carStatsViewer

import android.app.Activity
import android.app.PendingIntent
import android.car.Car
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.RawContacts.Data
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

/**
 * A simple activity that demonstrates connecting to car API and processing car property change
 * events.
 *
 * <p>Please see https://developer.android.com/reference/android/car/packages for API documentation.
 */

class MainActivity : Activity() {
    companion object {
        private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED)
    }
    private lateinit var timerHandler: Handler

    private val updateActivityTask = object : Runnable {
        override fun run() {
            InAppLogger.deepLog("MainActivity.updateActivityTask")
            updateActivity()
            timerHandler.postDelayed(this, 500)
        }
    }

    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    private lateinit var dataCollectorIntent: Intent

    override fun onResume() {
        super.onResume()

        if (AppPreferences.consumptionUnit) {
            main_consumption_plot.primaryUnitString = "Wh/km"
            DataHolder.consumptionPlotLine.HighlightFormat = "%.0f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.0f"
            DataHolder.consumptionPlotLine.Divider = 1f
        } else {
            main_consumption_plot.primaryUnitString = "kWh/100km"
            DataHolder.consumptionPlotLine.HighlightFormat = "%.1f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.1f"
            DataHolder.consumptionPlotLine.Divider = 10f
        }

        InAppLogger.log("MainActivity.onResume")
        //checkPermissions()
    }

    override fun onPause() {
        super.onPause()
        InAppLogger.log("MainActivity.onPause")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("MainActivity.onCreate")

        context = applicationContext

        val sharedPref = context.getSharedPreferences(
            getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )
        AppPreferences.consumptionUnit = sharedPref.getBoolean(getString(R.string.preferences_consumption_unit_key), false)
        AppPreferences.notifications = sharedPref.getBoolean(getString(R.string.preferences_notifications_key), false)
        AppPreferences.debug = sharedPref.getBoolean(getString(R.string.preferences_debug_key), false)
        AppPreferences.consumptionPlot = sharedPref.getBoolean(getString(R.string.preferences_consumption_plot_key), false)
        AppPreferences.deepLog = sharedPref.getBoolean(getString(R.string.preferences_deep_log_key), true)

        dataCollectorIntent = Intent(this, DataCollector::class.java)
        starterIntent = intent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mainActivityPendingIntent = pendingIntent
        startService(dataCollectorIntent)

        checkPermissions()

        setContentView(R.layout.activity_main)

        if (AppPreferences.consumptionPlot) main_consumption_plot_container.visibility = View.VISIBLE

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(DataHolder.consumptionPlotLine)
        main_consumption_plot.displayItemCount = 101
        main_consumption_plot.invalidate()

        main_button_reset.setOnClickListener {
            resetStats()
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        main_radio_group_distance.check(main_radio_10.id)
        main_radio_group_distance.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                main_radio_10.id -> main_consumption_plot.displayItemCount = 101
                main_radio_25.id -> main_consumption_plot.displayItemCount = 251
                main_radio_50.id -> main_consumption_plot.displayItemCount = 501
            }
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateActivityTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InAppLogger.log("onRequestPermissionResult")
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            finish()
            startActivity(starterIntent)
            resetStats()
        }
    }

    private fun updateActivity() {
        InAppLogger.logUIUpdate()
        /** Use data from DataHolder to Update MainActivity text */

        if (AppPreferences.consumptionPlot && main_consumption_plot_container.visibility == View.GONE) {
            main_consumption_plot_container.visibility = View.VISIBLE
        } else if (!AppPreferences.consumptionPlot && main_consumption_plot_container.visibility == View.VISIBLE) {
            main_consumption_plot_container.visibility = View.GONE
        }

        if (DataHolder.newPlotValueAvailable) {
            main_consumption_plot.invalidate()
            DataHolder.newPlotValueAvailable = false
        }

        chargePortConnectedTextView.text = DataHolder.chargePortConnected.toString()
        if (DataHolder.currentPowermW > 0) currentPowerTextView.setTextColor(Color.RED)
        else currentPowerTextView.setTextColor(Color.GREEN)
        currentPowerTextView.text = String.format("%.1f kW", DataHolder.currentPowermW / 1000000)
        if (AppPreferences.consumptionUnit) {
            usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        } else {
            usedEnergyTextView.text = String.format("%.1f kWh", DataHolder.usedEnergy / 1000)
        }
        currentSpeedTextView.text = String.format("%d km/h", (DataHolder.currentSpeed*3.6).toInt())
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        batteryEnergyTextView.text = String.format("%d/%d, %d%%", DataHolder.currentBatteryCapacity, DataHolder.maxBatteryCapacity, ((DataHolder.currentBatteryCapacity.toFloat()/DataHolder.maxBatteryCapacity.toFloat())*100).toInt())
        if (AppPreferences.consumptionUnit) { // Use Wh/km
            if (DataHolder.currentSpeed > 0) currentInstConsTextView.text = String.format("%d Wh/km", ((DataHolder.currentPowermW / 1000) / (DataHolder.currentSpeed * 3.6)).toInt())
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%d Wh/km", (DataHolder.usedEnergy/(DataHolder.traveledDistance/1000)).toInt())
            else averageConsumptionTextView.text = "N/A"
        } else { // Use kWh/100km
            if (DataHolder.currentSpeed > 0) currentInstConsTextView.text = String.format("%.1f kWh/100km", ((DataHolder.currentPowermW / 1000) / (DataHolder.currentSpeed * 3.6))/10)
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%.1f kWh/100km", (DataHolder.usedEnergy/(DataHolder.traveledDistance/1000))/10)
            else averageConsumptionTextView.text = "N/A"
        }
    }

    private fun checkPermissions() {
        if(checkSelfPermission(Car.PERMISSION_ENERGY) == PackageManager.PERMISSION_GRANTED) {
            //your code here
        } else {
            requestPermissions(permissions, 0)
        }
        if(checkSelfPermission(Car.PERMISSION_SPEED) == PackageManager.PERMISSION_GRANTED) {
            //your code here
        } else {
            requestPermissions(permissions, 1)
        }
    }

    private fun resetStats() {
        InAppLogger.log("MainActivity.resetStats")
        main_consumption_plot.reset()
        DataHolder.consumptionPlotLine.addDataPoint(0f)
        firstPlotValueAdded = false
        DataHolder.traveledDistance = 0F
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        DataHolder.usedEnergy = 0F
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        DataHolder.averageConsumption = 0F
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
    }
}
