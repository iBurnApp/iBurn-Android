package com.gj.animalauto

import android.location.LocationProvider
import com.gj.animalauto.message.GjMessageFactory
import com.gj.animalauto.message.GjMessageGps
import com.gj.animalauto.message.GjMessageStatusResponse
import org.junit.Test

import org.junit.Assert.*
import java.nio.ByteBuffer

/**
 * Example local unit test, which will execute on the development machine (host).

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun testPacketParsing() {

        val buffer = ByteBuffer.allocate(1024)

        val timeTick = 0
        val localVehicleId = 2

        val statusData = GjMessageStatusResponse.createData(false, false, false, false, false)

        val gpsData = GjMessageGps.createData(
                System.currentTimeMillis().toInt(),
                42.0,
                -122.4,
                30.0)

        val gpsHalfSize = gpsData.limit() / 2


        buffer.put(statusData)

        System.out.println("Attempting to parse " + gpsHalfSize + " / " + gpsData.limit() + " bytes")
        buffer.put(gpsData.array(), 0, gpsHalfSize)

        val messages = GjMessageFactory.parseAll(buffer)

        assertEquals(messages.messages.size, 1)



        System.out.println("Parsed " + messages.messages.size + " messages")
    }
}