package com.gj.animalauto

import android.net.wifi.ScanResult

/**
 * Created by dbro on 7/19/17.
 */


fun getVehicleIconResId(vehicleId: Int, greyedIcon: Boolean = false): Int {
    return when (vehicleId) {
        1 -> if (greyedIcon) R.drawable.map_icon_lion_grey else R.drawable.map_icon_lion
        3 -> if (greyedIcon) R.drawable.map_icon_tiger_grey else R.drawable.map_icon_tiger
        4 -> if (greyedIcon) R.drawable.map_icon_zebra_grey else R.drawable.map_icon_zebra
        5 -> if (greyedIcon) R.drawable.map_icon_rhino_grey else R.drawable.map_icon_rhino
        else -> if (greyedIcon) R.drawable.map_icon_elephant_grey else R.drawable.map_icon_elephant
    }
}

fun getVehicleName(vehicleId: Int): String {
    when (vehicleId) {
        1 -> return "Lion"
        3 -> return "Tiger"
        4 -> return "Zebra"
        5 -> return "Rhino"
        else -> return "Elephant"
    }
}

/**
 * In order of priority, descending.
 * It's extremely unfortunate that the numeric part of the WiFi SSID doesn't correspond to the ids
 * the 900 Mhz radio system uses e.g: [getVehicleName]
 */
private val carWifiNetworks = arrayOf(
        CarWifiNetwork(ssid = "GJ_0_Camp", carId = 0),
        CarWifiNetwork(ssid = "GJ_1_Lion", carId = 1),
        CarWifiNetwork(ssid = "GJ_2_Zebra", carId = 4),
        CarWifiNetwork(ssid = "GJ_3_Rhino", carId = 5),
        CarWifiNetwork(ssid = "GJ_4_Tiger", carId = 3),
        CarWifiNetwork(ssid = "GJ_5_Elephant", carId = 2)
)

public fun isHigherPriorityCarPresentInWifiScan(scanResults: List<ScanResult>, localCarId: Int): Boolean {
    val highestPriorityInScan = getHighestPriorityCarIdPresentInScanResults(scanResults)
    if (highestPriorityInScan == -1) {
        // No others found
        return false
    } else {
        // Another car was found
        val localPriority = getPriorityForCarId(localCarId)
        return highestPriorityInScan > localPriority
    }
}

/**
 * Each vehicle has a priority so that when all cars are in proximity, only the highest priority
 * may control lights.
 */
public fun getHighestPriorityCarIdPresentInScanResults(scanResults: List<ScanResult>): Int {

    carWifiNetworks.forEachIndexed { priority, carWifiNetwork ->
        if (containsSsid(ssid = carWifiNetwork.ssid, scanResults = scanResults)) {

            return carWifiNetwork.carId
        }
    }

    return -1
}

private fun containsSsid(ssid: String, scanResults: List<ScanResult>): Boolean {
    return scanResults.find { it.SSID == ssid } != null
}

/**
 * @return the priority of the car with the given ssid or [Int.MAX_VALUE] if no priority is assigned. 0 is the highest priority
 */
private fun getPriorityForSsid(ssid: String): Int {
    val result = carWifiNetworks.indexOfFirst { it.ssid == ssid }
    return if (result == -1) {
        Int.MAX_VALUE
    } else {
        result
    }
}

/**
 * @return the priority of the car with the given Car Id or [Int.MAX_VALUE] if no priority is assigned. 0 is the highest priority
 */
private fun getPriorityForCarId(carId: Int): Int {
    val result = carWifiNetworks.indexOfFirst { it.carId == carId }
    return if (result == -1) {
        Int.MAX_VALUE
    } else {
        result
    }
}

private data class CarWifiNetwork(val ssid: String, val carId: Int)