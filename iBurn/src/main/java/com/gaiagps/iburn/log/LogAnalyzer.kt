package com.gaiagps.iburn.log

import java.io.File
import java.io.IOException
import java.util.regex.Pattern

/**
 * Functions to create simple summaries of App Logcat logs. e.g: "1 successful stream. 1 failed stream.\nStream 0: Successful stream\nStream 1: WiFi connection timed out"
 * Created by dbro on 2/2/17.
 */

const val numVehicleIds = 5 // This number is constant enough

// Per stream-attempt benchmarks
val BtConnectionIssued = "BtCar\$manageSocket : Connecting to"
val BtConnectionCompleted = "BtCar : Connected to"
val BtDataReceivedPattern = Pattern.compile("BtCar : Read ([0-9]+) bytes from ([a-zA-Z]+)")
val BtDataParsedPattern = Pattern.compile("BtCar : Parsed ([0-9]+) messages up to byte")

val GotGpsMessagePattern = Pattern.compile("MapboxMapFragment : Got gps message from vehicle id ([0-9]+)")


@Throws(IOException::class)
fun analyzeAppLog(appLog: File): AppLogReport {
    val logContents = appLog.readText()
    return analyzeAppLog(logContents)
}

fun analyzeAppLog(appLog: String): AppLogReport {

    val btConnectionIssued = appLog.contains(BtConnectionIssued)
    val btConnectionCompleted = appLog.contains(BtConnectionCompleted)

    val btDataReceived = BtDataReceivedPattern.matcher(appLog).find()
    val btDataParsed = BtDataParsedPattern.matcher(appLog).find()

    val gpsMatcher = GotGpsMessagePattern.matcher(appLog)
    val gotGpsMessage = gpsMatcher.find()

    val gpsFromIds = HashSet<Int>()

    if (gotGpsMessage) {
        while (gpsMatcher.find() && gpsFromIds.size < numVehicleIds) {
            gpsFromIds.add(gpsMatcher.group(1).toInt())
        }
    }

    return AppLogReport(btConnectionIssued = btConnectionIssued,
            btConnectionCompleted = btConnectionCompleted,
            btDataReceived = btDataReceived,
            btDataParsed = btDataParsed,
            gotGpsMessage = gotGpsMessage,
            gotGpsForVehicleIds = gpsFromIds)
}


data class AppLogReport(val btConnectionIssued: Boolean,
                        val btConnectionCompleted: Boolean,
                        val btDataReceived: Boolean,
                        val btDataParsed: Boolean,
                        val gotGpsMessage: Boolean,
                        val gotGpsForVehicleIds: Set<Int>) {


    override fun toString(): String {

        if (gotGpsMessage) {
            if (gotGpsForVehicleIds.size == numVehicleIds) {
                return "Got Gps Messages for all cars"
            } else {
                val idStrBuilder = StringBuilder()
                gotGpsForVehicleIds.forEachIndexed { index, i ->
                    idStrBuilder.append(i)
                    if (index < gotGpsForVehicleIds.size - 1) {
                        idStrBuilder.append(", ")
                    }
                }
                return "Got Gps Message for cars " + idStrBuilder.toString()
            }
        }

        if (btDataParsed) return "Got parseable BT data"

        if (btDataReceived) return "Got BT data, but did not parse"

        if (btConnectionCompleted) return "Got BT connection, but no data"

        if (btConnectionIssued) return "BT connection issued, did not succeed"

        return "BT connection wasn't issued. Was a primary car selected?"
    }
}