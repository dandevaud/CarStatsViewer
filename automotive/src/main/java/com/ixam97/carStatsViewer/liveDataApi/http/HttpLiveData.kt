package com.ixam97.carStatsViewer.liveDataApi.http

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.google.gson.Gson
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataCollector.DrivingState
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.roundToInt


class HttpLiveData (
    detailedLog : Boolean = true
): LiveDataApi("com.ixam97.carStatsViewer_dev.http_live_data_connection_broadcast", detailedLog) {


    private fun addBasicAuth(connection: HttpURLConnection, username: String, password: String) {
        if (username == ""  && password == "") {
            return
        }

        val encoded: String = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(StandardCharsets.UTF_8)) //Java 8

        connection.setRequestProperty("Authorization", "Basic $encoded")
    }

    private fun getConnection(url: URL, username: String, password: String) : HttpURLConnection {
        val con: HttpURLConnection = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8")
        con.setRequestProperty("Accept","application/json")
        con.connectTimeout = timeout
        con.readTimeout = timeout
        con.doOutput = true
        con.doInput = true

        addBasicAuth(con, username, password)

        return con
    }

    private fun isValidURL(possibleURL: CharSequence?): Boolean {
        if (possibleURL == null) {
            return false
        }

        if (!possibleURL.contains("https://"))
            return false

        return android.util.Patterns.WEB_URL.matcher(possibleURL).matches()
    }

    override fun showSettingsDialog(context: Context) {
        val layout = LayoutInflater.from(context).inflate(R.layout.dialog_http_live_data, null)
        val url = layout.findViewById<EditText>(R.id.http_live_data_url)
        val username = layout.findViewById<EditText>(R.id.http_live_data_username)
        val password = layout.findViewById<EditText>(R.id.http_live_data_password)
        val httpLiveDataEnabled = layout.findViewById<Switch>(R.id.http_live_data_enabled)

        val httpLiveDataSettingsDialog = AlertDialog.Builder(context).apply {
            setView(layout)

            setPositiveButton("OK") { _, _ ->
                AppPreferences(context).httpLiveDataURL = url.text.toString()
                AppPreferences(context).httpLiveDataUsername = username.text.toString()
                AppPreferences(context).httpLiveDataPassword = password.text.toString()
            }

            setTitle(context.getString(R.string.settings_apis_http))
            setMessage(context.getString(R.string.http_description))
            setCancelable(true)
            create()
        }

        val dialog = httpLiveDataSettingsDialog.show()

        httpLiveDataEnabled.isChecked = AppPreferences(context).httpLiveDataEnabled
        httpLiveDataEnabled.setOnClickListener {
            AppPreferences(context).httpLiveDataEnabled = httpLiveDataEnabled.isChecked
        }

        url.setText(AppPreferences(context).httpLiveDataURL)
        username.setText(AppPreferences(context).httpLiveDataUsername)
        password.setText(AppPreferences(context).httpLiveDataPassword)

        // Enable the Ok button initially only in case the user already entered a valid URL
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isValidURL(url.text.toString())

        url.addTextChangedListener(object : TextValidator(url) {
            override fun validate(textView: TextView?, text: String?) {
                if (text == null || textView == null) {
                    return
                }
                if (!isValidURL(text) && text.isNotEmpty()) {
                    textView.error = context.getString(R.string.http_invalid_url)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    return
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        })
    }

    override fun sendNow(realTimeData: RealTimeData) {

        if (!AppPreferences(CarStatsViewer.appContext).httpLiveDataEnabled) {
            connectionStatus = ConnectionStatus.UNUSED
            return
        }

        if (realTimeData.isInitialized()) {
            connectionStatus = send(
                HttpDataSet(
                    timestamp = System.currentTimeMillis(),
                    currentSpeed = realTimeData.speed!! * 3.6f,
                    currentPower = realTimeData.power!! / 1_000_000f,
                    currentGear = realTimeData.selectedGear,
                    chargePortConnected = realTimeData.chargePortConnected,
                    batteryLevel = realTimeData.batteryLevel,
                    stateOfCharge =  (realTimeData.stateOfCharge!! * 100f).roundToInt(),
                    currentIgnitionState = realTimeData.ignitionState,
                    instConsumption = realTimeData.instConsumption,
                    //avgConsumption = dataManager.avgConsumption,
                    //avgSpeed = dataManager.avgSpeed,
                    //travelTime = dataManager.travelTime,
                    //chargeTime = dataManager.chargeTime,
                    driveState = realTimeData.drivingState,
                    ambientTemperature = realTimeData.ambientTemperature,
                    maxBatteryLevel = realTimeData.batteryLevel,
                    //tripStartDate = dataManager.tripStartDate,
                    //usedEnergy = dataManager.usedEnergy,
                    //traveledDistance = dataManager.traveledDistance,
                    //chargeStartDate = dataManager.chargeStartDate,
                    //chargedEnergy = dataManager.chargedEnergy,
                    lat = realTimeData.lat,
                    lon = realTimeData.lon,
                    alt = realTimeData.alt,

                    // Helpers
                    isCharging = realTimeData.chargePortConnected,
                    isParked = (realTimeData.drivingState == DrivingState.PARKED || realTimeData.drivingState == DrivingState.CHARGE),
                    isFastCharging = ((realTimeData.chargePortConnected?:false) && ((realTimeData.power?:0f) < -11_000_000f)),

                    // ABRP debug
                    abrpPackage = (CarStatsViewer.liveDataApis[0] as AbrpLiveData).lastPackage
                )
            )
        }
    }

    private fun send(dataSet: HttpDataSet, context: Context = CarStatsViewer.appContext): ConnectionStatus {
        val username = AppPreferences(context).httpLiveDataUsername
        val password = AppPreferences(context).httpLiveDataPassword
        val responseCode: Int

        val gson = Gson()
        val liveDataJson = gson.toJson(dataSet)

        try {
            val url = URL(AppPreferences(context).httpLiveDataURL) // + "?json=$jsonObject")
            val connection = getConnection(url, username, password)
            DataOutputStream(connection.outputStream).apply {
                writeBytes(liveDataJson)
                flush()
                close()
            }
            responseCode = connection.responseCode

            if (detailedLog) {
                var logString = "HTTP Webhook: Status: ${connection.responseCode}, Msg: ${connection.responseMessage}, Content:"
                logString += try {
                    connection.inputStream.bufferedReader().use {it.readText()}

                } catch (e: java.lang.Exception) {
                    "No response content"
                }
                if (dataSet.lat == null) logString += ". No valid location!"
                InAppLogger.d(logString)
            }

            connection.inputStream.close()
            connection.disconnect()
        } catch (e: java.net.SocketTimeoutException) {
            InAppLogger.e("HTTP Webhook: Network timeout error")
            return ConnectionStatus.ERROR
        } catch (e: java.lang.Exception) {
            InAppLogger.e("HTTP Webhook: Connection error")
            return ConnectionStatus.ERROR
        }

        if (responseCode != 200) {
            InAppLogger.e("HTTP Webhook: Transmission failed. Status code $responseCode")
            return ConnectionStatus.ERROR
        }

        return ConnectionStatus.CONNECTED
    }
}

